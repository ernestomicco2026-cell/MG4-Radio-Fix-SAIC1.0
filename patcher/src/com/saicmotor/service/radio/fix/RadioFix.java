package com.saicmotor.service.radio.fix;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.radio.RadioDabInfo;
import android.hardware.radio.RadioManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.saicmotor.sdk.radio.bean.RadioBean;
import com.saicmotor.service.radio.RadioService;
import com.saicmotor.service.radio.storage.RadioStorage;
import com.saicmotor.service.radio.utils.ServiceInformationBean;
import com.saicmotor.service.radio.utils.ServiceLogoInfoBean;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Correzioni per il servizio radio MG4 EH32 EU (com.saicmotor.service.radio).
 * Ogni metodo pubblico e' chiamato da un piccolo hook smali nel codice originale.
 * Ogni hook e' protetto da try/catch: in caso di errore si torna al comportamento originale.
 */
public final class RadioFix {
    public static final String TAG = "RadioFix";

    private static final int MAX_SLIDES = 4;
    private static final int MAX_LOGO_ENTRIES = 200;
    private static final int ART_SIZE = 320;
    private static final long FOCUS_LOSS_WINDOW_MS = 4000;
    private static final long SCAN_TIMEOUT_MS = 90000;
    private static final long CACHE_MIN_INTERVAL_MS = 60000;
    /** Dopo un nostro tune/step i ProgramInfo in arrivo sono "eco" della sintonia, non un cambio AF. */
    private static final long TUNE_ECHO_WINDOW_MS = 5000;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private static volatile boolean sScanning = false;
    private static volatile long sScanStart = 0;
    private static volatile long sLastFocusLoss = 0;
    private static volatile boolean sBootResumeDone = false;
    private static volatile long sLastTune = 0;
    private static volatile boolean sAutoScanDone = false;
    private static volatile Object sPendingCb;
    private static volatile int sPendingType;
    private static volatile RadioBean sBeforeScan;
    private static final long SEARCH_TIMEOUT_MS = 90000;
    private static final Map<Integer, Long> sLastNamed = new HashMap<Integer, Long>();
    private static final long RENAME_MIN_INTERVAL_MS = 30 * 60 * 1000L;
    private static volatile RadioService sService;
    private static final Map<String, Long> sLastCacheSave = new HashMap<String, Long>();

    private RadioFix() {}

    private static void log(String s) { Log.i(TAG, s); }

    private static void warn(String s, Throwable t) { Log.w(TAG, s, t); }

    // ---------------------------------------------------------------- FM: lista stazioni

    /** Chiamato all'avvio di una scansione AM/FM (RadioTunerExt.scan). */
    public static void onScanStart() {
        sScanning = true;
        sScanStart = System.currentTimeMillis();
        sLastTune = sScanStart;
        log("scan start");
    }

    /** Chiamato a ogni tune/step/seek AM/FM chiesto dal servizio (RadioTunerExt). */
    public static void onTune() {
        sLastTune = System.currentTimeMillis();
    }

    /** Chiamato quando la lista scansionata viene salvata (RadioService.amOrFmScanOver). */
    public static void onScanOver(RadioService svc) {
        if (sScanning) log("scan over");
        sScanning = false;
        try {
            guardList(svc, "mFmRadios", 2);
            guardList(svc, "mAmRadios", 1);
        } catch (Throwable t) {
            warn("onScanOver", t);
        }
        // vale anche per le richieste DAB, per cui il firmware fa la ricerca AM
        finishSearch(sPendingType, "fine ricerca");
        if (sBeforeScan == null) {
            final RadioService s2 = svc;
            MAIN.postDelayed(new Runnable() {
                @Override public void run() { if (!sScanning) reapplyRdsConfig(s2); }
            }, 1500);
        }
        retuneAfterScan(svc);
    }

    /** Fine della ricerca DAB (RadioService.scanDabOver, chiamato a ogni lista DAB ricevuta). */
    public static void onDabScanOver() {
        if (sPendingType != 4) return;
        final Object cb = sPendingCb;
        MAIN.postDelayed(new Runnable() {
            @Override public void run() {
                if (sPendingCb == cb) finishSearch(4, "lista DAB ricevuta");
            }
        }, 4000);
    }

    // ---------------------------------------------------------------- risposta "ricerca finita" all'app Radio

    /**
     * L'app Radio, quando chiede una ricerca, BLOCCA i tocchi (pulsanti, lista, impostazioni) finche'
     * il servizio non chiama ITuneCallback.onFinished. Il firmware non la chiama mai: qui la chiamiamo
     * a fine ricerca, o dopo SEARCH_TIMEOUT_MS se la ricerca non da' segni di vita.
     */
    private static void rememberSearch(final Object cb, int type) {
        if (cb == null) return;
        Object prev = sPendingCb;
        if (prev != null && prev != cb) callFinished(prev, "sostituita da una nuova ricerca");
        sPendingCb = cb;
        sPendingType = type;
        MAIN.postDelayed(new Runnable() {
            @Override public void run() {
                if (sPendingCb == cb) {
                    sPendingCb = null;
                    callFinished(cb, "tempo scaduto");
                }
            }
        }, SEARCH_TIMEOUT_MS);
    }

    private static void finishSearch(int type, String why) {
        Object cb = sPendingCb;
        if (cb == null || sPendingType != type) return;
        sPendingCb = null;
        callFinished(cb, why);
    }

    private static void callFinished(Object cb, String why) {
        try {
            ((com.saicmotor.sdk.radio.ITuneCallback) cb).onFinished(true);
            log("ricerca finita (" + why + "): app Radio sbloccata");
        } catch (Throwable t) {
            warn("callFinished", t);
        }
    }

    // ---------------------------------------------------------------- impostazioni RDS (AF, TA, regione...)

    private static String cfgText(Object cfg) {
        if (cfg == null) return "null";
        StringBuilder b = new StringBuilder();
        for (String g : new String[] {"getAF", "getTA", "getRegion", "getDist", "getServiceFollowing"}) {
            try {
                b.append(g.substring(3)).append('=').append(cfg.getClass().getMethod(g).invoke(cfg)).append(' ');
            } catch (Throwable t) {
                b.append(g.substring(3)).append("=? ");
            }
        }
        return b.toString().trim();
    }

    private static final Runnable NOTIFY_CFG = new Runnable() {
        @Override public void run() {
            try {
                RadioService svc = sService;
                if (svc == null || svc.getAmFmRadioTuner() == null) return;
                Object cfg = field(svc.getAmFmRadioTuner(), "rdsConfig");
                if (cfg == null) return;
                Object bean = Class.forName("com.saicmotor.service.radio.utils.RadioWrapper")
                        .getMethod("rdsBox", Class.forName("android.hardware.radio.RadioManager$RdsConfig"))
                        .invoke(null, cfg);
                svc.notifyRdsConfig((com.saicmotor.sdk.radio.bean.RdsConfigBean) bean);
                log("impostazioni RDS inviate all'app Radio: " + cfgText(cfg));
            } catch (Throwable t) {
                warn("notifyCfg", t);
            }
        }
    };

    /**
     * Subito dopo RadioTuner.setRdsConfig (RadioTunerExt.getFmRdsConfig, chiamato da ogni modifica
     * di AF/TA/regione/distanza/service following). L'app Radio allinea gli interruttori a quello
     * che il sintonizzatore conferma: se la conferma non arriva o arriva vecchia, l'interruttore
     * torna indietro. Qui, poco dopo, mandiamo all'app lo stato salvato dal servizio.
     */
    public static void onRdsConfigApplied(int result, Object cfg) {
        try {
            log("setRdsConfig -> " + result + " (" + cfgText(cfg) + ")");
            MAIN.removeCallbacks(NOTIFY_CFG);
            MAIN.postDelayed(NOTIFY_CFG, 1500);
        } catch (Throwable t) {
            warn("onRdsConfigApplied", t);
        }
    }

    /** Inizio di RadioService$6.onRadioConfigChanged: solo log (cosa conferma il sintonizzatore). */
    public static void onHalRdsConfig(Object cfg) {
        log("sintonizzatore conferma impostazioni RDS: " + cfgText(cfg));
    }

    /** Dopo una ricerca: la ricerca puo' azzerare la configurazione RDS nel sintonizzatore. */
    private static void reapplyRdsConfig(RadioService svc) {
        try {
            if (svc == null || svc.getAmFmRadioTuner() == null) return;
            log("riapplico le impostazioni RDS dopo la ricerca");
            svc.getAmFmRadioTuner().getFmRdsConfig();
        } catch (Throwable t) {
            warn("reapplyRdsConfig", t);
        }
    }

    /** Dopo una ricerca FM partita da noi torna sulla stazione che si stava ascoltando. */
    private static void retuneAfterScan(final RadioService svc) {
        final RadioBean prev = sBeforeScan;
        sBeforeScan = null;
        if (svc == null || prev == null || prev.getRadioType() != 2 || prev.getRadioState() != 1) return;
        final int f = prev.getFrequencyKhz();
        if (f < 87500 || f > 108000) return;
        MAIN.postDelayed(new Runnable() {
            @Override public void run() {
                try {
                    if (sScanning) return;
                    RadioBean cur = svc.getCurrentRadioInfo();
                    if (cur != null && cur.getRadioType() != 2) return; // l'utente e' passato a un'altra banda
                    log("dopo la ricerca torno su " + f);
                    svc.tune(2, f);
                    reapplyRdsConfig(svc);
                } catch (Throwable t) {
                    warn("retuneAfterScan", t);
                }
            }
        }, 1000);
    }

    /**
     * A fine scansione il firmware SOSTITUISCE la lista salvata con quella appena raccolta.
     * Se la nuova e' molto piu' corta (es. 1 stazione al posto di 29: scansione di sottofondo
     * interrotta o solo l'eco della stazione in ascolto) la scartiamo e teniamo la vecchia.
     */
    @SuppressWarnings("rawtypes")
    private static void guardList(RadioService svc, String field, int type) throws Exception {
        Object o = field(svc, field);
        if (!(o instanceof List)) return;
        List found = (List) o;
        int n = found.size();
        if (n == 0) return;
        List<RadioManager.ProgramInfo> stored = RadioStorage.getInstance().getValidRadios(type).toList();
        int old = stored == null ? 0 : stored.size();
        boolean accept = old <= 1 || (n >= 3 && n * 3 >= old);
        if (accept) {
            log("scan list " + (type == 2 ? "FM" : "AM") + ": " + n + " stazioni (prima " + old + ")");
        } else {
            log("scan list " + (type == 2 ? "FM" : "AM") + " scartata: " + n + " stazioni contro " + old + " salvate");
            found.clear();
        }
    }

    @SuppressWarnings("rawtypes")
    private static void clearTemp(int type) {
        try {
            RadioService svc = sService;
            if (svc == null) return;
            Object o = field(svc, type == 2 ? "mFmRadios" : "mAmRadios");
            if (o instanceof List) ((List) o).clear();
        } catch (Throwable t) {
            warn("clearTemp", t);
        }
    }

    // ---------------------------------------------------------------- ricerca FM a comando / automatica

    /**
     * Inizio di RadioServiceOptions.scan (comando "cerca" via AIDL). Il firmware ignora la banda e
     * fa sempre la ricerca AM. Con la banda FM facciamo la ricerca FM.
     * @return true se gestito qui (il codice originale non viene eseguito).
     */
    public static boolean scanRequest(int radioType, boolean forward, Object callback) {
        try {
            // AM: ricerca AM originale. DAB: il firmware fa anche qui la ricerca AM (la funzione DAB
            // esiste ma lascia l'audio in muto): non la cambiamo, ma la risposta "finita" arriva.
            rememberSearch(callback, radioType);
            if (radioType != 2) return false;
            boolean ok = startFmScan("comando cerca", forward);
            if (!ok) finishSearch(2, "sintonizzatore non pronto");
            return ok;
        } catch (Throwable t) {
            warn("scanRequest", t);
            return false;
        }
    }

    private static boolean startFmScan(final String why, final boolean forward) {
        final RadioService svc = sService;
        if (svc == null || svc.getAmFmRadioTuner() == null) return false;
        log("ricerca FM: " + why);
        try {
            RadioBean cur = svc.getCurrentRadioInfo();
            if (cur != null && cur.getRadioType() == 2 && cur.getRadioState() == 1) {
                RadioBean copy = new RadioBean();
                copy.setRadioType(2);
                copy.setFrequencyKhz(cur.getFrequencyKhz());
                copy.setRadioState(1);
                sBeforeScan = copy;
            }
        } catch (Throwable t) {
            warn("startFmScan prev", t);
        }
        IO.execute(new Runnable() {
            @Override public void run() {
                try {
                    svc.getAmFmRadioTuner().scan(2, forward);
                } catch (Throwable t) {
                    warn("startFmScan", t);
                }
            }
        });
        return true;
    }

    /** Una volta per accensione: se la lista FM ha 0 o 1 stazioni e si ascolta l'FM, cerca da sola. */
    private static void maybeAutoScan(RadioBean bean) {
        try {
            if (sAutoScanDone || sScanning || sService == null || bean == null || bean.getRadioType() != 2) return;
            List<RadioManager.ProgramInfo> stored = RadioStorage.getInstance().getValidRadios(2).toList();
            int n = stored == null ? 0 : stored.size();
            if (n > 1) return;
            sAutoScanDone = true;
            log("lista FM con " + n + " stazioni: ricerca automatica tra 8 s");
            MAIN.postDelayed(new Runnable() {
                @Override public void run() {
                    if (!sScanning) startFmScan("lista FM vuota", true);
                }
            }, 8000);
        } catch (Throwable t) {
            warn("maybeAutoScan", t);
        }
    }

    /**
     * Il firmware originale aggiunge alla lista stazioni ogni ProgramInfo ricevuto, anche fuori
     * dalla scansione (es. dopo un seek): 3 s dopo la lista salvata viene SOSTITUITA da quell'unica
     * stazione e avanti/indietro restano bloccati su di essa. Qui accettiamo i risultati solo
     * durante una scansione.
     *
     * Fuori dalla scansione un ProgramInfo FM con una frequenza diversa da quella in ascolto, che
     * non sia l'eco di un nostro tune, e' il sintonizzatore che ha cambiato frequenza da solo
     * (AF/RDS: stessa stazione su una frequenza con segnale migliore). Il firmware originale lo
     * "seguiva" solo per effetto collaterale (sostituendo la lista); ora lo seguiamo davvero,
     * aggiornando la stazione corrente senza toccare la lista.
     */
    public static boolean shouldCollect(RadioManager.ProgramInfo info) {
        try {
            if (info == null) return false;
            long value = info.getSelector().getPrimaryId().getValue();
            int type = info.getSelector().getProgramType();
            if (value == 0) {
                // Marcatore di inizio scansione dell'HAL (anche per le ricerche di sottofondo).
                // Il firmware qui CANCELLA la lista salvata: non lo facciamo, svuotiamo solo la
                // lista temporanea. La lista salvata viene sostituita a fine scansione, se sensata.
                onScanStart();
                clearTemp(type);
                return false;
            }
            if (sScanning && System.currentTimeMillis() - sScanStart > SCAN_TIMEOUT_MS) {
                sScanning = false;
            }
            if (sScanning) return true;
            // fuori dalla scansione non si aggiunge nulla alla lista (nemmeno se e' vuota:
            // diventerebbe una lista di una sola stazione). Se e' vuota parte la ricerca automatica.
            if (type == 2) {
                String ps = psOf(info);
                if (ps != null) nameStation(khz(value), ps, info);
                followAf(info);
            }
            else log("ignored program info outside scan: " + value + " type " + type);
            return false;
        } catch (Throwable t) {
            warn("shouldCollect", t);
            return true;
        }
    }

    // ---------------------------------------------------------------- FM: nomi RDS nella lista

    private static int khz(long v) {
        return (int) (v < 87500 ? v * 10 : v);
    }

    private static String psOf(RadioManager.ProgramInfo info) {
        try {
            android.hardware.radio.RadioMetadata m = info == null ? null : info.getMetadata();
            String ps = m == null ? null : m.getString("android.hardware.radio.metadata.RDS_PS");
            if (ps == null) return null;
            ps = ps.trim();
            return ps.isEmpty() ? null : ps;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Inizio di RadioService$6.onRdsMainInfoChanged (nome RDS della stazione in ascolto).
     * Il firmware mette il nome sulla stazione corrente, ma subito dopo l'osservatore la
     * sostituisce con la voce della lista: se la voce non ha nome si vede solo la frequenza.
     * Qui scriviamo il nome anche nella voce della lista.
     */
    public static void onRdsMain(Object mainInfo) {
        try {
            if (mainInfo == null) return;
            Object f = mainInfo.getClass().getMethod("getFrequency").invoke(mainInfo);
            Object psRaw = mainInfo.getClass().getMethod("getPsName").invoke(mainInfo);
            if (!(f instanceof Number) || psRaw == null) return;
            String ps = psRaw instanceof char[] ? new String((char[]) psRaw) : String.valueOf(psRaw);
            ps = ps.replace('\u0000', ' ').trim();
            if (ps.isEmpty() || "null".equals(ps)) return;
            long v = ((Number) f).longValue();
            int k = (int) (v < 20000 ? v * 10 : v); // l'HAL usa unita' da 10 kHz
            nameStation(k, ps, null);
        } catch (Throwable t) {
            warn("onRdsMain", t);
        }
    }

    /**
     * Scrive il nome RDS nella voce della lista FM con quella frequenza (se c'e': la lista non
     * si allunga). Un nome gia' presente si aggiorna al massimo ogni 30 minuti, perche' alcune
     * stazioni cambiano il nome di continuo e ogni aggiornamento riscrive la lista salvata.
     */
    private static void nameStation(int k, String ps, RadioManager.ProgramInfo source) {
        try {
            if (sScanning) return;
            List<RadioManager.ProgramInfo> stored = RadioStorage.getInstance().getValidRadios(2).toList();
            if (stored == null || stored.isEmpty()) return;
            int idx = -1;
            for (int i = 0; i < stored.size(); i++) {
                if (khz(stored.get(i).getSelector().getPrimaryId().getValue()) == k) { idx = i; break; }
            }
            if (idx < 0) return;
            RadioManager.ProgramInfo entry = stored.get(idx);
            String old = psOf(entry);
            if (ps.equals(old)) return;
            long now = System.currentTimeMillis();
            synchronized (sLastNamed) {
                Long last = sLastNamed.get(k);
                if (old != null && last != null && now - last < RENAME_MIN_INTERVAL_MS) return;
                sLastNamed.put(k, now);
            }
            RadioManager.ProgramInfo named = source != null ? source : withName(entry, ps);
            if (named == null) return;
            List<RadioManager.ProgramInfo> merged = new java.util.ArrayList<RadioManager.ProgramInfo>(stored);
            merged.set(idx, named);
            RadioStorage.getInstance().addAmFmRadio(merged, 2);
            log("RDS nome " + k + ": " + (old == null ? "-" : old) + " -> " + ps);
        } catch (Throwable t) {
            warn("nameStation", t);
        }
    }

    /** Copia della voce con il nome RDS nei metadati (costruttore nascosto: riflessione). */
    private static RadioManager.ProgramInfo withName(RadioManager.ProgramInfo e, String ps) {
        java.lang.reflect.Constructor<RadioManager.ProgramInfo> c;
        android.hardware.radio.RadioMetadata meta;
        try {
            c = RadioManager.ProgramInfo.class.getConstructor(
                    android.hardware.radio.ProgramSelector.class,
                    android.hardware.radio.ProgramSelector.Identifier.class,
                    android.hardware.radio.ProgramSelector.Identifier.class,
                    java.util.Collection.class, int.class, int.class,
                    android.hardware.radio.RadioMetadata.class, Map.class);
            android.hardware.radio.RadioMetadata.Builder b;
            try {
                android.hardware.radio.RadioMetadata m = e.getMetadata();
                b = m != null ? new android.hardware.radio.RadioMetadata.Builder(m)
                              : new android.hardware.radio.RadioMetadata.Builder();
            } catch (Throwable t) {
                b = new android.hardware.radio.RadioMetadata.Builder();
            }
            meta = b.putString("android.hardware.radio.metadata.RDS_PS", ps).build();
        } catch (Throwable t) {
            warn("withName", t);
            return null;
        }
        try {
            return c.newInstance(e.getSelector(), e.getLogicallyTunedTo(), e.getPhysicallyTunedTo(),
                    e.getRelatedContent(), 0, e.getSignalStrength(), meta, e.getVendorInfo());
        } catch (Throwable t) {
            try { // come fa il firmware in AmFmRadioOptions.addCollection
                return c.newInstance(e.getSelector(), null, null, null, 0, 0, meta, null);
            } catch (Throwable t2) {
                warn("withName", t2);
                return null;
            }
        }
    }

    /** Aggiorna la stazione FM corrente quando il sintonizzatore passa da solo a un'altra frequenza. */
    private static void followAf(RadioManager.ProgramInfo info) {
        try {
            RadioService svc = sService;
            if (svc == null) return;
            RadioBean found = boxFm(info);
            if (found == null) return;
            int freq = found.getFrequencyKhz();
            RadioBean cur = svc.getCurrentRadioInfo();
            if (cur == null || cur.getRadioType() != 2) return;
            int was = cur.getFrequencyKhz();
            if (freq == was) return; // eco della frequenza in ascolto
            if (freq < 87500 || freq > 108000) return;
            long since = System.currentTimeMillis() - sLastTune;
            if (since < TUNE_ECHO_WINDOW_MS) {
                log("ignored tune echo " + freq + " (current " + was + ", " + since + " ms after tune)");
                return;
            }
            found.setRadioState(cur.getRadioState());
            log("AF follow " + was + " -> " + freq + " (" + found.getRadioName() + ")");
            svc.updateCurrentRadio(found);
        } catch (Throwable t) {
            warn("followAf", t);
        }
    }

    /**
     * ProgramInfo FM -> RadioBean come fa il firmware (RadioWrapper.box: nome RDS, preferito...).
     * Chiamato per riflessione perche' RadioWrapper usa classi nascoste del framework.
     */
    private static RadioBean boxFm(RadioManager.ProgramInfo info) {
        try {
            Object b = Class.forName("com.saicmotor.service.radio.utils.RadioWrapper")
                    .getMethod("box", RadioManager.ProgramInfo.class).invoke(null, info);
            if (b instanceof RadioBean) return (RadioBean) b;
        } catch (Throwable t) {
            warn("boxFm", t);
        }
        long v = info.getSelector().getPrimaryId().getValue();
        RadioBean b = new RadioBean();
        b.setRadioType(2);
        b.setFrequencyKhz((int) (v < 87500 ? v * 10 : v));
        return b;
    }

    // ---------------------------------------------------------------- DAB: loghi

    /** Il firmware accetta solo loghi "logo_unrestricted" perfettamente quadrati. */
    public static boolean acceptLogo(RadioDabInfo.LogoInfo logo) {
        try {
            if (logo == null) return false;
            String type = logo.getLogoType();
            if (!"logo_unrestricted".equals(type)) return false;
            return logo.getWidth() > 0 && logo.getHeight() > 0;
        } catch (Throwable t) {
            warn("acceptLogo", t);
            return false;
        }
    }

    /** Sostituisce BitmapFactory.decodeByteArray per i loghi: restituisce sempre un quadrato. */
    public static Bitmap decodeLogo(byte[] data, int offset, int len) {
        Bitmap b = null;
        try {
            b = BitmapFactory.decodeByteArray(data, offset, len);
            return square(b);
        } catch (Throwable t) {
            warn("decodeLogo", t);
            return b;
        }
    }

    /** Aggiunta con deduplica e limite alle liste mLogoList / mServiceInformation. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static boolean addCapped(List list, Object item) {
        try {
            if (list == null) return false;
            if (item instanceof ServiceLogoInfoBean) {
                String url = ((ServiceLogoInfoBean) item).getmUrl();
                for (int i = list.size() - 1; i >= 0; i--) {
                    Object o = list.get(i);
                    if (o instanceof ServiceLogoInfoBean && url != null
                            && url.equals(((ServiceLogoInfoBean) o).getmUrl())) {
                        list.remove(i);
                    }
                }
            } else if (item instanceof ServiceInformationBean) {
                ServiceInformationBean n = (ServiceInformationBean) item;
                for (int i = list.size() - 1; i >= 0; i--) {
                    Object o = list.get(i);
                    if (o instanceof ServiceInformationBean) {
                        ServiceInformationBean e = (ServiceInformationBean) o;
                        if (e.getmEnsembleId() == n.getmEnsembleId() && e.getmServiceId() == n.getmServiceId()
                                && eq(e.getmLogoUrl(), n.getmLogoUrl())) {
                            list.remove(i);
                        }
                    }
                }
            }
            boolean r = list.add(item);
            while (list.size() > MAX_LOGO_ENTRIES) list.remove(0);
            return r;
        } catch (Throwable t) {
            warn("addCapped", t);
            try { return list.add(item); } catch (Throwable t2) { return false; }
        }
    }

    private static boolean eq(Object a, Object b) { return a == null ? b == null : a.equals(b); }

    // ---------------------------------------------------------------- DAB: slideshow

    /**
     * Sostituisce RadioBean.addDabSlideShow: tiene solo le ultime immagini, manda l'immagine
     * alla schermata home (launcher) e la salva come copertina della stazione.
     */
    public static void onSlide(RadioService svc, RadioBean bean, Bitmap bitmap) {
        try {
            List<Bitmap> slides = bean.getDabSlideShow();
            if (slides != null) {
                slides.add(bitmap);
                while (slides.size() > MAX_SLIDES) slides.remove(0);
            } else {
                bean.addDabSlideShow(bitmap);
            }
        } catch (Throwable t) {
            warn("onSlide add", t);
            try { bean.addDabSlideShow(bitmap); } catch (Throwable ignored) { }
        }
        try {
            Bitmap art = square(bitmap);
            postToLauncher(svc, art);
            saveArt(svc, bean, art);
        } catch (Throwable t) {
            warn("onSlide art", t);
        }
    }

    /** Cambio stazione DAB: mostra subito l'ultima copertina salvata per quella stazione. */
    public static void onStationChanged(final RadioService svc) {
        MAIN.postDelayed(new Runnable() {
            @Override public void run() { restoreArt(svc); }
        }, 1500);
    }

    private static void restoreArt(final RadioService svc) {
        try {
            final RadioBean bean = svc.getCurrentRadioInfo();
            if (bean == null || bean.getRadioType() != 4) return;
            final File f = artFile(svc, bean);
            if (f == null || !f.exists()) return;
            final String key = key(bean);
            IO.execute(new Runnable() {
                @Override public void run() {
                    final Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath());
                    if (b == null) return;
                    MAIN.post(new Runnable() {
                        @Override public void run() {
                            try {
                                RadioBean cur = svc.getCurrentRadioInfo();
                                if (cur == null || !key.equals(key(cur))) return;
                                postToLauncher(svc, b);
                                List<Bitmap> slides = cur.getDabSlideShow();
                                if (slides != null && slides.isEmpty()) {
                                    slides.add(b);
                                    Object live = field(svc, "mCurrentProgram");
                                    if (live instanceof androidx.lifecycle.MutableLiveData) {
                                        ((androidx.lifecycle.MutableLiveData) live).postValue(cur);
                                    }
                                }
                                log("restored art " + key);
                            } catch (Throwable t) {
                                warn("restoreArt post", t);
                            }
                        }
                    });
                }
            });
        } catch (Throwable t) {
            warn("restoreArt", t);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void postToLauncher(RadioService svc, Bitmap art) throws Exception {
        if (art == null) return;
        Object live = field(svc, "mDabServiceLogoLiveDataOnLauncher");
        if (live instanceof androidx.lifecycle.MutableLiveData) {
            ((androidx.lifecycle.MutableLiveData) live).postValue(art);
        }
    }

    private static String key(RadioBean bean) {
        return bean.getEnsembleId() + "_" + bean.getServiceId();
    }

    private static File artFile(Context ctx, RadioBean bean) {
        if (bean.getServiceId() == 0) return null;
        File dir = new File(ctx.getFilesDir(), "dab_art");
        return new File(dir, "dab_" + key(bean) + ".jpg");
    }

    private static void saveArt(Context ctx, RadioBean bean, final Bitmap art) {
        final File f = artFile(ctx, bean);
        if (f == null || art == null) return;
        String k = key(bean);
        long now = System.currentTimeMillis();
        synchronized (sLastCacheSave) {
            Long last = sLastCacheSave.get(k);
            if (last != null && now - last < CACHE_MIN_INTERVAL_MS) return;
            sLastCacheSave.put(k, now);
        }
        IO.execute(new Runnable() {
            @Override public void run() {
                FileOutputStream out = null;
                try {
                    File dir = f.getParentFile();
                    if (!dir.exists()) dir.mkdirs();
                    File tmp = new File(dir, f.getName() + ".tmp");
                    out = new FileOutputStream(tmp);
                    art.compress(Bitmap.CompressFormat.JPEG, 85, out);
                    out.close();
                    out = null;
                    tmp.renameTo(f);
                    trimCache(dir);
                } catch (Throwable t) {
                    warn("saveArt", t);
                } finally {
                    if (out != null) try { out.close(); } catch (Throwable ignored) { }
                }
            }
        });
    }

    private static void trimCache(File dir) {
        File[] files = dir.listFiles();
        if (files == null || files.length <= 150) return;
        java.util.Arrays.sort(files, new java.util.Comparator<File>() {
            @Override public int compare(File a, File b) { return Long.compare(a.lastModified(), b.lastModified()); }
        });
        for (int i = 0; i < files.length - 150; i++) files[i].delete();
    }

    // ---------------------------------------------------------------- immagini quadrate

    /**
     * Rende l'immagine quadrata: se le bande laterali sono di un colore uniforme ritaglia il
     * centro, altrimenti la inserisce (senza deformarla) su un quadrato del colore del bordo.
     */
    public static Bitmap square(Bitmap src) {
        if (src == null) return null;
        int w = src.getWidth(), h = src.getHeight();
        if (w <= 0 || h <= 0) return src;
        if (w == h) return scaleDown(src);
        int side = Math.min(w, h);
        boolean wide = w > h;
        int band = (Math.max(w, h) - side) / 2;
        if (band > 0 && uniformBands(src, wide, band)) {
            Bitmap c = wide ? Bitmap.createBitmap(src, band, 0, side, side)
                            : Bitmap.createBitmap(src, 0, band, side, side);
            return scaleDown(c);
        }
        int big = Math.max(w, h);
        int out = Math.min(big, ART_SIZE);
        float scale = out / (float) big;
        Bitmap dst = Bitmap.createBitmap(out, out, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(dst);
        cv.drawColor(src.getPixel(0, 0) | 0xFF000000);
        int dw = Math.round(w * scale), dh = Math.round(h * scale);
        android.graphics.Rect r = new android.graphics.Rect((out - dw) / 2, (out - dh) / 2,
                (out - dw) / 2 + dw, (out - dh) / 2 + dh);
        cv.drawBitmap(src, null, r, new Paint(Paint.FILTER_BITMAP_FLAG));
        return dst;
    }

    private static Bitmap scaleDown(Bitmap b) {
        if (b.getWidth() <= ART_SIZE) return b;
        return Bitmap.createScaledBitmap(b, ART_SIZE, ART_SIZE, true);
    }

    private static boolean uniformBands(Bitmap b, boolean wide, int band) {
        int w = b.getWidth(), h = b.getHeight();
        int ref = b.getPixel(0, 0);
        int steps = 12;
        for (int i = 0; i <= steps; i++) {
            for (int side = 0; side < 2; side++) {
                int x, y;
                if (wide) {
                    x = side == 0 ? (band - 1) * i / steps : w - 1 - (band - 1) * i / steps;
                    for (int j = 0; j <= steps; j++) {
                        y = (h - 1) * j / steps;
                        if (!close(ref, b.getPixel(x, y))) return false;
                    }
                } else {
                    y = side == 0 ? (band - 1) * i / steps : h - 1 - (band - 1) * i / steps;
                    for (int j = 0; j <= steps; j++) {
                        x = (w - 1) * j / steps;
                        if (!close(ref, b.getPixel(x, y))) return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean close(int a, int b) {
        return Math.abs(Color.red(a) - Color.red(b)) < 24 && Math.abs(Color.green(a) - Color.green(b)) < 24
                && Math.abs(Color.blue(a) - Color.blue(b)) < 24;
    }

    // ---------------------------------------------------------------- ripresa all'avvio

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("radiofix", Context.MODE_PRIVATE);
    }

    /** Chiamato a inizio AudioController.onAudioFocusChange. */
    public static void onFocusChange(int focusChange) {
        if (focusChange < 0) sLastFocusLoss = System.currentTimeMillis();
    }

    /**
     * Chiamato a ogni cambio della stazione/stato corrente. Ricorda se l'utente sta ascoltando la
     * radio. Una pausa causata dallo spegnimento (perdita del focus audio) non conta come pausa.
     */
    public static void onProgram(RadioService svc, RadioBean bean) {
        try {
            if (bean == null) return;
            maybeAutoScan(bean);
            int st = bean.getRadioState();
            boolean focusLoss = System.currentTimeMillis() - sLastFocusLoss < FOCUS_LOSS_WINDOW_MS;
            SharedPreferences p = prefs(svc);
            boolean cur = p.getBoolean("playing", false);
            if (st == 1 && !cur) {
                p.edit().putBoolean("playing", true).apply();
            } else if (st == 2 && cur && !focusLoss) {
                p.edit().putBoolean("playing", false).apply();
                log("user paused -> no auto resume");
            }
        } catch (Throwable t) {
            warn("onProgram", t);
        }
    }

    /** Sostituisce lo stato salvato letto da LastMode.tuneDefault. */
    public static int resumeState(int savedState) {
        try {
            Context c = com.saicmotor.service.radio.RadioServiceApp.getContext();
            if (savedState != 1 && c != null && prefs(c).getBoolean("enabled", true)
                    && prefs(c).getBoolean("playing", false)) {
                log("auto resume: saved state " + savedState + " -> playing");
                return 1;
            }
        } catch (Throwable t) {
            warn("resumeState", t);
        }
        return savedState;
    }

    /** Alla creazione del servizio: tentativi di ripresa se il BOOT_COMPLETED arriva troppo presto/tardi. */
    public static void scheduleBootResume(final RadioService svc) {
        sService = svc;
        long[] delays = {6000, 14000};
        for (long d : delays) {
            MAIN.postDelayed(new Runnable() {
                @Override public void run() { bootResume(svc); }
            }, d);
        }
    }

    private static void bootResume(RadioService svc) {
        try {
            if (sBootResumeDone) return;
            RadioBean cur = svc.getCurrentRadioInfo();
            boolean playing = cur != null && cur.getRadioState() == 1
                    && svc.getAudioController() != null && svc.getAudioController().isRadioAudioFocus();
            if (playing) { sBootResumeDone = true; return; }
            if (svc.getAudioController() == null || !svc.getAudioController().isLastSource()) return;
            if (!prefs(svc).getBoolean("enabled", true) || !prefs(svc).getBoolean("playing", false)) return;
            log("boot resume");
            sBootResumeDone = true;
            svc.getLastMode().tuneDefault();
        } catch (Throwable t) {
            warn("bootResume", t);
        }
    }

    // ---------------------------------------------------------------- stabilita' (radiofix3)

    /**
     * Rete di sicurezza: un'eccezione non gestita in un thread di lavoro (pool "radio thread #N",
     * pool di Room) non fa piu' morire il servizio radio. Il thread di lavoro termina, il pool ne
     * crea un altro. Main thread e HandlerThread (callback del sintonizzatore) restano invariati:
     * li' l'errore va al gestore originale come prima.
     */
    public static void installCrashGuard() {
        try {
            final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
            if (prev != null && prev.getClass().getName().startsWith(RadioFix.class.getName())) return;
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override public void uncaughtException(Thread t, Throwable e) {
                    try {
                        if (isWorkerThread(t)) {
                            Log.e(TAG, "crash guard: eccezione nel thread " + t.getName()
                                    + " ignorata, il servizio resta attivo", e);
                            return;
                        }
                    } catch (Throwable ignored) { }
                    if (prev != null) prev.uncaughtException(t, e);
                }
            });
            log("crash guard attivo");
        } catch (Throwable t) {
            warn("installCrashGuard", t);
        }
    }

    private static boolean isWorkerThread(Thread t) {
        if (t == null || t == Looper.getMainLooper().getThread()) return false;
        String n = t.getName();
        if (n == null) return false;
        return n.startsWith("radio thread #") || n.startsWith("arch_disk_io_") || n.startsWith("pool-");
    }

    /**
     * Inizio di ProgramAlarm.setProgramReminder. Il firmware fa SimpleDateFormat.parse(getTime())
     * gestendo solo ParseException: con l'ora null va in NullPointerException e, poiche' il
     * promemoria resta nel DB, il servizio muore a ogni avvio. Qui un promemoria senza ora valida
     * o gia' passato non viene programmato e viene tolto dal DB.
     * @return true per proseguire con il codice originale.
     */
    public static boolean checkReminder(com.saicmotor.sdk.radio.bean.DabSubRadioBean bean) {
        try {
            if (bean == null) {
                log("reminder null ignorato");
                return false;
            }
            String time = bean.getTime();
            String why = null;
            if (time == null || time.trim().isEmpty()) {
                why = "senza ora";
            } else {
                java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
                f.setLenient(false);
                java.util.Date d = null;
                try { d = f.parse(time.trim()); } catch (Throwable ignored) { }
                if (d == null) why = "ora non valida (" + time + ")";
                else if (d.getTime() < System.currentTimeMillis()) why = "gia' passato (" + time + ")";
            }
            if (why == null) return true;
            log("reminder DAB " + why + ": rimosso (service " + bean.getServiceId()
                    + " ensemble " + bean.getEnsembleId() + ")");
            try {
                com.saicmotor.service.radio.utils.ProgramAlarm.getInstance().cancelProgramData(bean);
            } catch (Throwable t) {
                warn("checkReminder remove", t);
            }
            return false;
        } catch (Throwable t) {
            warn("checkReminder", t);
            return false; // nel dubbio non programmare: e' il caso che faceva crashare il servizio
        }
    }

    /**
     * Inizio di RadioManagerExt.initRadioConfig: con la lista moduli vuota (sintonizzatore non
     * ancora pronto) il firmware fa mModules.get(0) -> IndexOutOfBoundsException.
     * @return true per proseguire con il codice originale.
     */
    public static boolean hasModules(List<?> modules) {
        boolean ok = modules != null && !modules.isEmpty();
        if (!ok) log("nessun modulo radio: initRadioConfig saltato, il servizio riprovera'");
        return ok;
    }

    // ---------------------------------------------------------------- util

    private static Object field(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }
}
