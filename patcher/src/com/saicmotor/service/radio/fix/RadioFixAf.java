package com.saicmotor.service.radio.fix;

import android.content.SharedPreferences;
import android.hardware.radio.ProgramSelector;
import android.hardware.radio.RadioManager;

import com.saicmotor.sdk.radio.bean.RadioBean;
import com.saicmotor.service.radio.RadioService;
import com.saicmotor.service.radio.storage.RadioStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FM: cambio di frequenza alternativa (AF).
 *
 * 1) Segue i cambi di frequenza fatti dal sintonizzatore (modulo esterno, AF RDS): un ProgramInfo o un
 *    MainInfo RDS con una frequenza diversa da quella in ascolto e lo stesso PI (o, se il PI non e'
 *    noto, lo stesso nome PS) e' la stessa stazione su un'altra frequenza: si segue anche subito dopo
 *    una sintonia. L'"eco" e' solo la frequenza appena chiesta.
 * 2) AF attivo: se il segnale resta sotto soglia per 8 s, prova (a radio muta, circa 1 s) un'altra
 *    frequenza nota della stessa stazione e ci passa solo se il segnale e' nettamente migliore.
 *    Le frequenze di ogni stazione (per PI, o per nome) vengono ricordate da ricerche, ascolto e prove.
 *
 * Tutto e' protetto: un errore viene scritto nel log e il servizio continua come prima.
 */
final class RadioFixAf {

    private RadioFixAf() {}

    // ---------------------------------------------------------------- soglie (provvisorie, da tarare con i log)
    /** Segnale debole: sotto questo valore di RadioSignal.signalStrength. */
    static final int WEAK = 20;
    /** Isteresi: l'alternativa deve avere almeno questo segnale in piu'. */
    static final int HYST = 6;
    static final long WEAK_FOR_MS = 8000;
    static final long MIN_GAP_MS = 30000;
    static final long MAX_GAP_MS = 5 * 60 * 1000L;
    static final long PROBE_MS = 1200;
    static final long ECHO_WINDOW_MS = 5000;
    static final long WEAK_LOG_EVERY_MS = 10000;
    static final int FORMAT_LOGS = 5;

    // ---------------------------------------------------------------- stato
    private static volatile int sReqFreq = -1;      // ultima frequenza chiesta al sintonizzatore
    private static volatile long sReqAt = 0;
    private static volatile int sCurFreq = -1;      // stazione in ascolto
    private static volatile int sCurPi = -1;
    private static volatile String sCurPs;
    private static volatile int sSig = -1;          // ultimo segnale della stazione in ascolto
    private static volatile int sSigFreq = -1;
    private static volatile String sSigText = "";
    private static volatile long sWeakSince = 0;
    private static volatile long sLastWeakLog = 0;
    private static volatile int sFormatLogs = 0;
    private static volatile int sSignalFormatLogs = 0;

    // cambio seguito, in attesa di sapere se il segnale li' e' buono per aggiornare la lista
    private static volatile int sPendOld = -1, sPendNew = -1;
    private static volatile String sPendPs;
    private static volatile long sPendAt = 0;

    // AF attivo
    private static volatile boolean sProbing = false;
    private static volatile int sProbeFreq = -1;
    private static volatile int sProbeOrigin = -1;
    private static volatile int sProbeOriginSig = -1;
    private static volatile int sProbeBestSig = -1;
    private static volatile int sProbePi = -1;
    private static volatile String sProbePs;
    private static volatile long sNextAttempt = 0;
    private static volatile long sGap = MIN_GAP_MS;
    private static volatile int sRotate = 0;
    private static volatile long sNoCandLog = 0;

    // memoria: chiave "P<pi>" o "S<nome>" -> frequenza -> ultimo segnale (-1 se non noto)
    private static final Map<String, Map<Integer, Integer>> MEM = new LinkedHashMap<String, Map<Integer, Integer>>();
    private static volatile boolean sMemLoaded = false;
    private static volatile boolean sMemDirty = false;

    // ---------------------------------------------------------------- formato ProgramInfo

    private static int idType(ProgramSelector.Identifier id) {
        try { return id == null ? -1 : id.getType(); } catch (Throwable t) { return -1; }
    }

    private static long firstId(ProgramSelector sel, int type) {
        try { return sel.getFirstId(type); } catch (Throwable t) { return -1; }
    }

    /** Frequenza FM in kHz: dalla frequenza "fisica", poi dall'identificativo frequenza, poi dal primario. */
    static int freqOf(RadioManager.ProgramInfo info) {
        try {
            ProgramSelector.Identifier phy = info.getPhysicallyTunedTo();
            if (idType(phy) == 1) return RadioFix.khz(phy.getValue());
        } catch (Throwable ignored) { }
        try {
            ProgramSelector sel = info.getSelector();
            ProgramSelector.Identifier p = sel.getPrimaryId();
            if (idType(p) == 1) return RadioFix.khz(p.getValue());
            long f = firstId(sel, 1);
            if (f > 0) return RadioFix.khz(f);
            return RadioFix.khz(p.getValue());
        } catch (Throwable t) {
            return -1;
        }
    }

    /** PI RDS (tipo identificativo 2), -1 se non presente. */
    static int piOf(RadioManager.ProgramInfo info) {
        try {
            ProgramSelector sel = info.getSelector();
            ProgramSelector.Identifier p = sel.getPrimaryId();
            if (idType(p) == 2) return (int) p.getValue();
            long pi = firstId(sel, 2);
            if (pi > 0) return (int) pi;
            ProgramSelector.Identifier log = info.getLogicallyTunedTo();
            if (idType(log) == 2) return (int) log.getValue();
        } catch (Throwable ignored) { }
        return -1;
    }

    private static String pi(int pi) {
        return pi > 0 ? Integer.toHexString(pi).toUpperCase() : "?";
    }

    private static void logFormat(RadioManager.ProgramInfo info) {
        if (sFormatLogs >= FORMAT_LOGS) return;
        sFormatLogs++;
        try {
            ProgramSelector sel = info.getSelector();
            ProgramSelector.Identifier p = sel.getPrimaryId();
            ProgramSelector.Identifier phy = info.getPhysicallyTunedTo();
            ProgramSelector.Identifier lg = info.getLogicallyTunedTo();
            RadioFix.log("formato ProgramInfo: primario tipo " + idType(p) + " = " + p.getValue()
                    + ", fisico " + (phy == null ? "-" : idType(phy) + " = " + phy.getValue())
                    + ", logico " + (lg == null ? "-" : idType(lg) + " = " + lg.getValue())
                    + ", PI " + pi(piOf(info)) + ", segnale " + RadioFix.signal(info)
                    + ", PS " + RadioFix.psOf(info));
        } catch (Throwable t) {
            RadioFix.warn("logFormat", t);
        }
    }

    // ---------------------------------------------------------------- sintonia

    /** RadioTunerExt.tune(ProgramSelector): la frequenza chiesta (per riconoscere le eco). */
    static void onTuneSelector(Object selector) {
        try {
            if (!(selector instanceof ProgramSelector)) return;
            ProgramSelector sel = (ProgramSelector) selector;
            if (sel.getProgramType() != 2) return;
            ProgramSelector.Identifier p = sel.getPrimaryId();
            int f = idType(p) == 1 ? RadioFix.khz(p.getValue()) : (int) RadioFix.khz(firstId(sel, 1));
            sReqFreq = f;
            sReqAt = System.currentTimeMillis();
            if (sProbing || RadioFix.sVerifying || RadioFix.sScanning) return;
            if (f != sCurFreq) setCurrent(f, "sintonia");
        } catch (Throwable t) {
            RadioFix.warn("onTuneSelector", t);
        }
    }

    private static void setCurrent(int f, String why) {
        sCurFreq = f;
        sCurPi = -1;
        sCurPs = null;
        sSig = -1;
        sWeakSince = 0;
        sGap = MIN_GAP_MS;
        sNextAttempt = 0;
        // PI e nome gia' noti per questa frequenza
        loadMem();
        synchronized (MEM) {
            for (Map.Entry<String, Map<Integer, Integer>> e : MEM.entrySet()) {
                if (e.getKey().startsWith("P") && e.getValue().containsKey(f)) {
                    try { sCurPi = Integer.parseInt(e.getKey().substring(1), 16); } catch (Throwable ignored) { }
                    break;
                }
            }
        }
        try {
            List<RadioManager.ProgramInfo> stored = RadioStorage.getInstance().getValidRadios(2).toList();
            if (stored != null) {
                for (RadioManager.ProgramInfo i : stored) {
                    if (freqOf(i) == f) { sCurPs = RadioFix.psOf(i); break; }
                }
            }
        } catch (Throwable ignored) { }
    }

    private static boolean sameStation(int pi, String ps) {
        if (pi > 0 && sCurPi > 0) return pi == sCurPi;
        return ps != null && sCurPs != null && RadioFix.nameKey(ps).equals(RadioFix.nameKey(sCurPs));
    }

    // ---------------------------------------------------------------- notizie dal sintonizzatore

    /**
     * ProgramInfo FM fuori dalla ricerca (al posto del vecchio followAf).
     */
    static void onProgramInfo(RadioManager.ProgramInfo info) {
        try {
            if (info == null) return;
            logFormat(info);
            int f = freqOf(info);
            int piv = piOf(info);
            String ps = RadioFix.psOf(info);
            int sig = RadioFix.signal(info);
            if (f >= 87500 && f <= 108000) remember(f, piv, ps, sig > 0 ? sig : -1);
            if (sProbing) {
                probeSeen(f, piv, ps, sig > 0 ? sig : -1);
                return;
            }
            if (RadioFix.sVerifying) return;
            if (f < 87500 || f > 108000) {
                RadioFix.log("cambio frequenza scartato: fuori banda (valore " + f + ", PI " + pi(piv) + ")");
                return;
            }
            RadioBean cur = currentFm();
            if (cur == null) return;
            if (sCurFreq < 0) setCurrent(cur.getFrequencyKhz(), "avvio");
            if (f == sCurFreq) {
                if (piv > 0) sCurPi = piv;
                if (ps != null) sCurPs = ps;
                return;
            }
            long since = System.currentTimeMillis() - sReqAt;
            boolean inWindow = since < ECHO_WINDOW_MS;
            if (inWindow && f == sReqFreq) {
                RadioFix.log("cambio frequenza scartato: eco della sintonia su " + f);
                return;
            }
            boolean same = sameStation(piv, ps);
            boolean known = (piv > 0 && sCurPi > 0) || (ps != null && sCurPs != null);
            if (!same && (inWindow || known)) {
                RadioFix.log("cambio frequenza scartato: " + f + " (PI " + pi(piv) + ", " + ps + ") non e' la stazione in ascolto "
                        + sCurFreq + " (PI " + pi(sCurPi) + ", " + sCurPs + ")" + (inWindow ? ", " + since + " ms dopo una sintonia" : ""));
                return;
            }
            follow(f, piv, ps, info, same ? "AF del sintonizzatore" : "AF del sintonizzatore (PI e nome non noti)");
        } catch (Throwable t) {
            RadioFix.warn("onProgramInfo AF", t);
        }
    }

    /** MainInfo RDS (frequenza, PI, nome) fuori dalla verifica. */
    static void onMainInfo(int f, int piv, String ps) {
        try {
            if (f >= 87500 && f <= 108000) remember(f, piv, ps, -1);
            if (sProbing) {
                probeSeen(f, piv, ps, -1);
                return;
            }
            if (RadioFix.sVerifying || RadioFix.sScanning) return;
            RadioBean cur = currentFm();
            if (cur == null) return;
            if (sCurFreq < 0) setCurrent(cur.getFrequencyKhz(), "avvio");
            if (f == sCurFreq) {
                boolean changed = piv > 0 && piv != sCurPi;
                if (piv > 0) sCurPi = piv;
                if (ps != null) sCurPs = ps;
                if (changed) RadioFix.log("in ascolto " + f + " " + ps + " PI " + pi(piv));
                return;
            }
            if (f < 87500 || f > 108000) return;
            if (System.currentTimeMillis() - sReqAt < ECHO_WINDOW_MS && f == sReqFreq) return;
            if (piv > 0 && sCurPi > 0 && piv == sCurPi) {
                follow(f, piv, ps, null, "AF del sintonizzatore (da RDS)");
            }
        } catch (Throwable t) {
            RadioFix.warn("onMainInfo AF", t);
        }
    }

    /** RadioService$6.onSignalInfoChanged(RadioSignal). */
    static void onSignal(Object rs) {
        try {
            if (rs == null) return;
            if (sSignalFormatLogs < 3) {
                sSignalFormatLogs++;
                RadioFix.log("formato segnale: " + rs);
            }
            int s = getInt(rs, "getSignalStrength");
            String text = "segnale " + s + " (qualita' " + getInt(rs, "getQuality") + ", usn " + getInt(rs, "getUsn")
                    + ", wam " + getInt(rs, "getWam") + ", offset " + getInt(rs, "getOffset") + ")";
            if (sProbing) {
                if (s > sProbeBestSig) sProbeBestSig = s;
                return;
            }
            if (RadioFix.sVerifying || RadioFix.sScanning) return;
            RadioBean cur = currentFm();
            if (cur == null) { sWeakSince = 0; return; }
            if (sCurFreq < 0 || cur.getFrequencyKhz() != sCurFreq) setCurrent(cur.getFrequencyKhz(), "stazione corrente");
            onSignalValue(s, text);
        } catch (Throwable t) {
            RadioFix.warn("onSignal", t);
        }
    }

    private static int getInt(Object o, String m) {
        try { return ((Number) o.getClass().getMethod(m).invoke(o)).intValue(); } catch (Throwable t) { return -1; }
    }

    private static void onSignalValue(int s, String text) {
        long now = System.currentTimeMillis();
        sSig = s;
        sSigFreq = sCurFreq;
        sSigText = text;
        remember(sCurFreq, sCurPi, sCurPs, s);
        // un cambio seguito diventa la frequenza della stazione in lista solo se li' il segnale e' buono
        if (sPendNew == sCurFreq && now - sPendAt < 20000) {
            if (s >= WEAK) {
                RadioFix.replaceStationFreq(sPendOld, sPendNew, sPendPs);
                sPendNew = -1;
            }
        } else if (sPendNew > 0 && now - sPendAt >= 20000) {
            sPendNew = -1;
        }
        if (s < 0 || s >= WEAK) {
            sWeakSince = 0;
            return;
        }
        if (sWeakSince == 0) sWeakSince = now;
        if (now - sLastWeakLog >= WEAK_LOG_EVERY_MS) {
            sLastWeakLog = now;
            RadioFix.log("in ascolto " + sCurFreq + " " + sCurPs + " PI " + pi(sCurPi) + ": " + text + " sotto soglia " + WEAK);
        }
        if (now - sWeakSince >= WEAK_FOR_MS && now >= sNextAttempt) {
            RadioFix.MAIN.post(new Runnable() { @Override public void run() { tryActiveAf(); } });
        }
    }

    private static RadioBean currentFm() {
        RadioService svc = RadioFix.sService;
        if (svc == null) return null;
        RadioBean cur = svc.getCurrentRadioInfo();
        if (cur == null || cur.getRadioType() != 2) return null;
        return cur;
    }

    /** La stazione in ascolto e' passata a un'altra frequenza: aggiorna la stazione corrente. */
    private static void follow(int f, int piv, String ps, RadioManager.ProgramInfo info, String why) {
        RadioService svc = RadioFix.sService;
        RadioBean cur = currentFm();
        if (svc == null || cur == null) return;
        int old = cur.getFrequencyKhz();
        RadioBean nb = null;
        if (info != null) nb = RadioFix.boxFm(info);
        if (nb == null) {
            nb = new RadioBean();
            nb.setRadioType(2);
            if (ps != null) { nb.setRds(true); nb.setRadioName(ps); }
        }
        nb.setFrequencyKhz(f);
        nb.setRadioState(cur.getRadioState());
        String name = ps != null ? ps : sCurPs;
        RadioFix.log("cambio frequenza seguito (" + why + "): " + old + " -> " + f + " " + name + " PI " + pi(piv > 0 ? piv : sCurPi));
        int keepPi = piv > 0 ? piv : sCurPi;
        sCurFreq = f;
        sCurPi = keepPi;
        if (name != null) sCurPs = name;
        sSig = -1;
        sWeakSince = 0;
        sPendOld = old;
        sPendNew = f;
        sPendPs = name;
        sPendAt = System.currentTimeMillis();
        svc.updateCurrentRadio(nb);
    }

    // ---------------------------------------------------------------- AF attivo

    private static boolean afEnabled(RadioService svc) {
        try {
            Object cfg = RadioFix.field(svc.getAmFmRadioTuner(), "rdsConfig");
            return cfg != null && Boolean.TRUE.equals(cfg.getClass().getMethod("getAF").invoke(cfg));
        } catch (Throwable t) {
            return false;
        }
    }

    private static void tryActiveAf() {
        try {
            long now = System.currentTimeMillis();
            if (sProbing || RadioFix.sScanning || RadioFix.sVerifying || now < sNextAttempt) return;
            if (sWeakSince == 0 || now - sWeakSince < WEAK_FOR_MS) return;
            final RadioService svc = RadioFix.sService;
            RadioBean cur = currentFm();
            if (svc == null || cur == null || cur.getRadioState() != 1 || svc.getAmFmRadioTuner() == null) return;
            if (!afEnabled(svc)) return;
            final int origin = cur.getFrequencyKhz();
            if (origin != sCurFreq) return;
            List<int[]> cands = candidates(origin);
            if (cands.isEmpty()) {
                if (now - sNoCandLog > 120000) {
                    sNoCandLog = now;
                    RadioFix.log("AF attivo: nessuna altra frequenza nota per " + origin + " " + sCurPs + " PI " + pi(sCurPi));
                }
                sNextAttempt = now + MIN_GAP_MS;
                return;
            }
            StringBuilder b = new StringBuilder();
            for (int[] c : cands) b.append(c[0]).append(" (segnale ").append(c[1] < 0 ? "?" : String.valueOf(c[1])).append(") ");
            // una sola prova per tentativo (interruzione di circa 1 s); a giro le altre
            int[] pick = cands.get(sRotate % cands.size());
            sRotate++;
            RadioFix.log("AF attivo: " + origin + " " + sCurPs + " PI " + pi(sCurPi) + " con segnale " + sSig
                    + " da " + ((now - sWeakSince) / 1000) + " s. Candidati: " + b.toString().trim() + ". Provo " + pick[0]);
            sProbing = true;
            sProbeOrigin = origin;
            sProbeOriginSig = sSig;
            sProbeFreq = pick[0];
            sProbeBestSig = -1;
            sProbePi = -1;
            sProbePs = null;
            svc.getAmFmRadioTuner().setMuted(true);
            svc.getAmFmRadioTuner().tune(com.saicmotor.service.radio.platform.ProgramSelectorExt.createAmFmSelector(pick[0]));
            svc.getAmFmRadioTuner().setMuted(true);
            RadioFix.MAIN.postDelayed(new Runnable() {
                @Override public void run() { endProbe(svc); }
            }, PROBE_MS);
        } catch (Throwable t) {
            RadioFix.warn("tryActiveAf", t);
            abortProbe("errore");
        }
    }

    private static void probeSeen(int f, int piv, String ps, int sig) {
        if (f != sProbeFreq) return;
        if (piv > 0) sProbePi = piv;
        if (ps != null) sProbePs = ps;
        if (sig > sProbeBestSig) sProbeBestSig = sig;
    }

    private static void endProbe(RadioService svc) {
        if (!sProbing) return;
        try {
            int origin = sProbeOrigin, cand = sProbeFreq, cs = sProbeBestSig, os = sProbeOriginSig;
            RadioBean cur = svc.getCurrentRadioInfo();
            if (cur == null || cur.getRadioType() != 2 || cur.getFrequencyKhz() != origin) {
                // l'utente ha cambiato stazione o banda durante la prova: non lo riportiamo indietro
                sProbing = false;
                RadioFix.log("AF attivo: prova interrotta, stazione cambiata dall'utente");
                return;
            }
            boolean samePi = sProbePi > 0 && sCurPi > 0 ? sProbePi == sCurPi
                    : (sProbePs != null && sCurPs != null && RadioFix.nameKey(sProbePs).equals(RadioFix.nameKey(sCurPs)))
                      || (sProbePi < 0 && sProbePs == null); // nessun RDS in 1 s: decide il segnale
            boolean better = cs >= WEAK && cs >= os + HYST;
            if (cs > 0) rememberSig(cand, cs);
            String detail = cand + " segnale " + (cs < 0 ? "?" : String.valueOf(cs)) + " PI " + pi(sProbePi) + " " + sProbePs
                    + " contro " + origin + " segnale " + os;
            sProbing = false;
            if (better && samePi) {
                RadioFix.log("AF attivo: passo a " + detail
                        + (sProbePi < 0 && sProbePs == null ? " (RDS non arrivato in tempo: frequenza nota per questa stazione)" : ""));
                String name = sCurPs != null ? sCurPs : sProbePs;
                int keepPi = sCurPi > 0 ? sCurPi : sProbePi;
                svc.tune(2, cand); // passa da onTuneSelector, che azzera nome e PI: li rimettiamo dopo
                RadioFix.unmute(svc);
                sCurFreq = cand;
                sCurPi = keepPi;
                sCurPs = name;
                sSig = cs;
                sWeakSince = 0;
                sGap = MIN_GAP_MS;
                sNextAttempt = System.currentTimeMillis() + MIN_GAP_MS;
                RadioFix.replaceStationFreq(origin, cand, name);
            } else {
                RadioFix.log("AF attivo: resto su " + origin + " (" + (samePi ? "segnale non migliore" : "stazione diversa") + "): " + detail);
                svc.getAmFmRadioTuner().tune(com.saicmotor.service.radio.platform.ProgramSelectorExt.createAmFmSelector(origin));
                RadioFix.unmute(svc);
                sGap = Math.min(sGap * 2, MAX_GAP_MS);
                sNextAttempt = System.currentTimeMillis() + sGap;
            }
        } catch (Throwable t) {
            RadioFix.warn("endProbe", t);
            abortProbe("errore");
        }
    }

    private static void abortProbe(String why) {
        if (!sProbing) return;
        sProbing = false;
        RadioService svc = RadioFix.sService;
        try {
            if (svc != null && sProbeOrigin > 0) {
                svc.getAmFmRadioTuner().tune(com.saicmotor.service.radio.platform.ProgramSelectorExt.createAmFmSelector(sProbeOrigin));
                RadioFix.unmute(svc);
            }
        } catch (Throwable t) {
            RadioFix.warn("abortProbe", t);
        }
        sNextAttempt = System.currentTimeMillis() + MAX_GAP_MS;
        RadioFix.log("AF attivo: prova annullata (" + why + ")");
    }

    /** Frequenze note della stazione in ascolto (stesso PI, altrimenti stesso nome), la migliore prima. */
    private static List<int[]> candidates(int origin) {
        loadMem();
        Map<Integer, Integer> all = new HashMap<Integer, Integer>();
        synchronized (MEM) {
            if (sCurPi > 0) {
                Map<Integer, Integer> m = MEM.get("P" + Integer.toHexString(sCurPi));
                if (m != null) all.putAll(m);
            }
            if (all.isEmpty() && sCurPs != null) {
                Map<Integer, Integer> m = MEM.get("S" + RadioFix.nameKey(sCurPs));
                if (m != null) all.putAll(m);
            }
        }
        List<int[]> out = new ArrayList<int[]>();
        for (Map.Entry<Integer, Integer> e : all.entrySet()) {
            int f = e.getKey();
            if (Math.abs(f - origin) < 150) continue; // stessa frequenza o canale adiacente
            out.add(new int[] {f, e.getValue()});
        }
        Collections.sort(out, new Comparator<int[]>() {
            @Override public int compare(int[] a, int[] b) { return b[1] - a[1]; }
        });
        while (out.size() > 4) out.remove(out.size() - 1);
        return out;
    }

    // ---------------------------------------------------------------- memoria delle frequenze

    static void remember(int f, int piv, String ps, int sig) {
        if (f < 87500 || f > 108000) return;
        if (piv <= 0 && ps == null) return;
        loadMem();
        synchronized (MEM) {
            if (piv > 0) put("P" + Integer.toHexString(piv), f, sig);
            if (ps != null) put("S" + RadioFix.nameKey(ps), f, sig);
        }
    }

    private static void rememberSig(int f, int sig) {
        synchronized (MEM) {
            for (Map<Integer, Integer> m : MEM.values()) if (m.containsKey(f)) m.put(f, sig);
            sMemDirty = true;
        }
        saveMemSoon();
    }

    private static void put(String key, int f, int sig) {
        Map<Integer, Integer> m = MEM.remove(key);
        if (m == null) m = new LinkedHashMap<Integer, Integer>();
        Integer old = m.get(f);
        if (old == null || (sig >= 0 && !old.equals(sig))) {
            m.put(f, sig >= 0 ? sig : (old == null ? -1 : old));
            sMemDirty = true;
        }
        while (m.size() > 8) m.remove(m.keySet().iterator().next());
        MEM.put(key, m); // in fondo: le piu' recenti restano
        while (MEM.size() > 120) {
            MEM.remove(MEM.keySet().iterator().next());
            sMemDirty = true;
        }
        if (sMemDirty) saveMemSoon();
    }

    private static final Runnable SAVE = new Runnable() {
        @Override public void run() {
            try {
                RadioService svc = RadioFix.sService;
                if (svc == null || !sMemDirty) return;
                StringBuilder b = new StringBuilder();
                synchronized (MEM) {
                    for (Map.Entry<String, Map<Integer, Integer>> e : MEM.entrySet()) {
                        b.append(e.getKey().replace(";", "").replace(":", "").replace(",", "").replace("/", "")).append(':');
                        for (Map.Entry<Integer, Integer> fe : e.getValue().entrySet())
                            b.append(fe.getKey()).append('/').append(fe.getValue()).append(',');
                        b.append(';');
                    }
                    sMemDirty = false;
                }
                RadioFix.prefs(svc).edit().putString("afmem", b.toString()).apply();
            } catch (Throwable t) {
                RadioFix.warn("saveMem", t);
            }
        }
    };

    private static void saveMemSoon() {
        RadioFix.MAIN.removeCallbacks(SAVE);
        RadioFix.MAIN.postDelayed(SAVE, 60000);
    }

    private static void loadMem() {
        if (sMemLoaded) return;
        RadioService svc = RadioFix.sService;
        if (svc == null) return;
        sMemLoaded = true;
        try {
            String s = RadioFix.prefs(svc).getString("afmem", "");
            synchronized (MEM) {
                for (String ent : s.split(";")) {
                    int c = ent.indexOf(':');
                    if (c <= 1) continue;
                    Map<Integer, Integer> m = new LinkedHashMap<Integer, Integer>();
                    for (String fs : ent.substring(c + 1).split(",")) {
                        int sl = fs.indexOf('/');
                        if (sl <= 0) continue;
                        try { m.put(Integer.parseInt(fs.substring(0, sl)), Integer.parseInt(fs.substring(sl + 1))); } catch (Throwable ignored) { }
                    }
                    if (!m.isEmpty()) MEM.put(ent.substring(0, c), m);
                }
            }
            RadioFix.log("AF: memoria caricata, " + MEM.size() + " stazioni con frequenze note");
        } catch (Throwable t) {
            RadioFix.warn("loadMem", t);
        }
    }

    // ---------------------------------------------------------------- avvio: impostazioni RDS

    /** All'avvio: impostazioni RDS del servizio e flag AF standard del sintonizzatore (RDS_AF = 4). */
    static void checkRdsAtBoot() {
        try {
            RadioService svc = RadioFix.sService;
            if (svc == null || svc.getAmFmRadioTuner() == null) return;
            loadMem();
            Object cfg = RadioFix.field(svc.getAmFmRadioTuner(), "rdsConfig");
            boolean af = afEnabled(svc);
            RadioFix.log("impostazioni RDS all'avvio: " + cfgTextOf(cfg));
            syncAfFlag(svc, af, "avvio");
        } catch (Throwable t) {
            RadioFix.warn("checkRdsAtBoot", t);
        }
    }

    static String cfgTextOf(Object cfg) {
        if (cfg == null) return "null";
        StringBuilder b = new StringBuilder();
        for (String g : new String[] {"getAF", "getTA", "getRegion", "getDist", "getServiceFollowing"}) {
            try { b.append(g.substring(3)).append('=').append(cfg.getClass().getMethod(g).invoke(cfg)).append(' '); }
            catch (Throwable t) { b.append(g.substring(3)).append("=? "); }
        }
        return b.toString().trim();
    }

    /** Allinea il flag standard RDS_AF del sintonizzatore all'impostazione AF (se il sintonizzatore lo supporta). */
    static void syncAfFlag(RadioService svc, boolean af, String why) {
        try {
            Object tuner = RadioFix.field(svc.getAmFmRadioTuner(), "mRadioTuner");
            if (tuner == null) return;
            boolean sup = (Boolean) tuner.getClass().getMethod("isConfigFlagSupported", int.class).invoke(tuner, 4);
            if (!sup) {
                RadioFix.log("flag RDS_AF del sintonizzatore: non supportato (" + why + ")");
                return;
            }
            boolean set = (Boolean) tuner.getClass().getMethod("isConfigFlagSet", int.class).invoke(tuner, 4);
            RadioFix.log("flag RDS_AF del sintonizzatore: supportato, attivo=" + set + ", impostazione AF=" + af + " (" + why + ")");
            if (set != af) {
                tuner.getClass().getMethod("setConfigFlag", int.class, boolean.class).invoke(tuner, 4, af);
                RadioFix.log("flag RDS_AF del sintonizzatore impostato a " + af);
            }
        } catch (Throwable t) {
            RadioFix.log("flag RDS_AF del sintonizzatore: non leggibile (" + t.getClass().getSimpleName() + ")");
        }
    }

    static boolean isProbing() {
        return sProbing;
    }
}
