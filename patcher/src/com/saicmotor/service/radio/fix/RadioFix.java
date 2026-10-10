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
    private static final long SCAN_TIMEOUT_MS = 180000;
    private static final long CACHE_MIN_INTERVAL_MS = 60000;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private static volatile boolean sScanning = false;
    private static volatile long sScanStart = 0;
    private static volatile long sLastFocusLoss = 0;
    private static volatile boolean sBootResumeDone = false;
    private static final Map<String, Long> sLastCacheSave = new HashMap<String, Long>();

    private RadioFix() {}

    private static void log(String s) { Log.i(TAG, s); }

    private static void warn(String s, Throwable t) { Log.w(TAG, s, t); }

    // ---------------------------------------------------------------- FM: lista stazioni

    /** Chiamato all'avvio di una scansione AM/FM (RadioTunerExt.scan). */
    public static void onScanStart() {
        sScanning = true;
        sScanStart = System.currentTimeMillis();
        log("scan start");
    }

    /** Chiamato quando la lista scansionata viene salvata (RadioService.amOrFmScanOver). */
    public static void onScanOver() {
        if (sScanning) log("scan over");
        sScanning = false;
    }

    /**
     * Il firmware originale aggiunge alla lista stazioni ogni ProgramInfo ricevuto, anche fuori
     * dalla scansione (es. dopo un seek): 3 s dopo la lista salvata viene SOSTITUITA da quell'unica
     * stazione e avanti/indietro restano bloccati su di essa. Qui accettiamo i risultati solo
     * durante una scansione (o se la lista e' vuota).
     */
    public static boolean shouldCollect(RadioManager.ProgramInfo info) {
        try {
            if (info == null) return false;
            long value = info.getSelector().getPrimaryId().getValue();
            int type = info.getSelector().getProgramType();
            if (value == 0) { // marcatore di inizio scansione inviato dall'HAL
                onScanStart();
                return true;
            }
            if (sScanning && System.currentTimeMillis() - sScanStart > SCAN_TIMEOUT_MS) {
                sScanning = false;
            }
            if (sScanning) return true;
            if (type == 1 || type == 2) {
                List<RadioManager.ProgramInfo> stored = RadioStorage.getInstance().getValidRadios(type).toList();
                if (stored == null || stored.isEmpty()) return true;
            }
            log("ignored program info outside scan: " + value + " type " + type);
            return false;
        } catch (Throwable t) {
            warn("shouldCollect", t);
            return true;
        }
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

    // ---------------------------------------------------------------- util

    private static Object field(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }
}
