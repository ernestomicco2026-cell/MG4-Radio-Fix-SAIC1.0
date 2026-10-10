# MG4 Radio Fix — SAIC 1.0 (EH32 EU)

Correzioni al **servizio radio originale** dell'infotainment MG4 con sistema **SAIC 1.0**
(allestimenti Comfort, Luxury, Trophy, XPower).

> ⚠️ **Non è per le versioni "Color Radio"** e non è per il firmware **EH32 LL**
> (dove la radio è un'unica app `com.saicmotor.radio`: per quello esiste
> [MG4-Radio-DABplus-Fix](https://github.com/Skittle6938/MG4-Radio-DABplus-Fix)).

Sviluppato e verificato su infotainment **SWI68-29958-1300R69**, servizio radio
`com.saicmotor.service.radio` versione **`1.0.0_EH32_SOP_2502201114`**.

---

## Cosa corregge

| Problema | Causa nel firmware originale | Correzione |
|---|---|---|
| **FM bloccato su una sola stazione** (es. resta sulla 90.90 e avanti/indietro non cambiano) | Ogni frequenza ricevuta, anche fuori dalla ricerca (es. dopo un seek), viene messa in una lista che 3 s dopo **sostituisce** l'intera lista stazioni | La lista si aggiorna solo durante una vera scansione |
| **La radio non riparte all'accensione** | Allo spegnimento la radio perde il focus audio e salva lo stato "in pausa" | Se stavi ascoltando la radio, all'avvio riparte; se l'avevi messa in pausa tu o usavi un'altra sorgente (CarPlay, Bluetooth…) resta ferma |
| **Loghi DAB che non compaiono** | Errore nel ciclo di abbinamento logo/stazione (`size() - 1`): l'ultima stazione ricevuta non viene mai controllata | Ciclo corretto |
| **Loghi DAB scartati** | Accettati solo loghi `logo_unrestricted` perfettamente quadrati | Accettati anche quelli rettangolari, resi quadrati (ritaglio delle bande uniformi o centratura) |
| **Nessuna immagine DAB+ sulla schermata home** | Lo slideshow va solo all'app radio | Le immagini dello slideshow vengono mandate anche alla home |
| **Attesa dell'immagine a ogni cambio stazione** | Nessuna memoria dello slideshow | L'ultima immagine di ogni stazione viene salvata e mostrata subito |
| **Memoria che cresce nel tempo** | Slideshow, loghi e info stazioni accumulati senza limite | Ultime 4 immagini, liste con deduplica e limite |

**Non tocca la grafica**: l'app con la schermata della radio (`com.saicmotor.hmi.radio`) resta quella di fabbrica.
**Non migliora la ricezione** (segnale debole, DAB instabile): quella dipende dal sintonizzatore e dal suo firmware.

---

## Compatibilità

| Firmware | Stato |
|---|---|
| SAIC 1.0, SWI68-29958-1300**R69**, servizio `1.0.0_EH32_SOP_2502201114` | ✅ [APK pronto](apk/) |
| SAIC 1.0, altre release (R59, R63, R67, R71…) | 🔧 Usa lo **script di build** sul tuo APK: verifica da solo se il codice è compatibile |
| Color Radio | ❌ Non supportato |(https://github.com/Skittle6938/MG4-Radio-DABplus-Fix) |

Controlla la versione del tuo servizio radio:

```bash
adb shell dumpsys package com.saicmotor.service.radio | grep versionName
```

Se è **`1.0.0_EH32_SOP_2502201114`** puoi usare l'APK pronto: **[scaricalo qui](https://github.com/ernestomicco2026-cell/MG4-Radio-Fix-SAIC1.0/raw/main/apk/RadioService_radiofix1_1.0.0_EH32_SOP_2502201114.apk)** (SHA-256 in [`apk/SHA256SUMS.txt`](apk/SHA256SUMS.txt)). Altrimenti costruisci il tuo (sotto).

---

## Installazione

### Opzione A — Da chiavetta USB (senza PC)

1. Scarica **[RadioService_radiofix1_1.0.0_EH32_SOP_2502201114.apk](https://github.com/ernestomicco2026-cell/MG4-Radio-Fix-SAIC1.0/raw/main/apk/RadioService_radiofix1_1.0.0_EH32_SOP_2502201114.apk)**
   e copialo su una chiavetta USB (FAT32 o exFAT).
2. Inserisci la chiavetta nella porta USB dell'auto.
3. Apri il **gestore file** dell'infotainment, tocca l'APK e conferma **Installa**
   (viene proposto come aggiornamento di un'app esistente).
4. **Riavvia l'unità** (vedi sotto).

### Opzione B — Con ADB

Serve ADB attivo sull'unità (per l'MG4: [ADB_util di Leon Kerman, thread XDA](https://xdaforums.com/t/mg4-electric-aaos-9-playing-and-possibly-other-mg-models.4697712/post-90591053)).

```bash
adb install -r RadioService_radiofix1_1.0.0_EH32_SOP_2502201114.apk
```

> **Dopo l'installazione RIAVVIA l'unità** (tieni premuto Home ~20 s). Il servizio radio è sempre in memoria:
> finché non riavvii continua a girare quello vecchio.

Dopo il primo avvio **rifai una ricerca FM** per ricostruire la lista stazioni completa.

L'APK ha lo stesso nome di pacchetto e la stessa firma dell'originale: si installa come **aggiornamento
dell'app di sistema**. Non serve root e la partizione di sistema non viene modificata.

### Tornare all'originale

```bash
adb uninstall com.saicmotor.service.radio
```

Poi riavvia: torna automaticamente la versione di fabbrica (rimane intatta in `/system`).

Senza PC: se le Impostazioni dell'auto mostrano l'elenco delle app, apri il servizio radio
(`com.saicmotor.service.radio`) e scegli **Disinstalla aggiornamenti**, poi riavvia.
Non tutte le versioni del firmware mostrano questa voce: in quel caso serve ADB.

---

## Costruire l'APK per la tua versione

Requisiti (Linux/macOS/WSL): Java 11+, git, curl, unzip, zip, python3, openssl.

```bash
# 1. scarica il servizio radio originale dalla tua auto
adb shell pm path com.saicmotor.service.radio
adb pull /system/priv-app/<cartella>/<file>.apk RadioService_originale.apk   # usa il percorso mostrato sopra

# 2. costruisci
cd patcher
./build.sh ../RadioService_originale.apk
```

Lo script scarica gli strumenti necessari, poi **si ferma senza produrre nulla** se:
- l'APK non è il servizio radio EH32 (`com.saicmotor.service.radio`);
- l'originale non è firmato con la chiave di piattaforma AOSP (l'aggiornamento non si installerebbe);
- il codice del tuo firmware è diverso da quello previsto (uno dei punti da modificare non si trova).

Se si ferma al terzo punto, apri una *issue* allegando il tuo APK originale e la versione SWI68.

---

## Verificare che funzioni

```bash
adb logcat -s RadioFix
```

Messaggi tipici: `scan start` / `scan over` (ricerca FM), `ignored program info outside scan`
(una stazione che prima avrebbe "bloccato" la lista), `auto resume`, `restored art`.

---

## Come funziona (tecnico)

Il servizio originale viene decompilato con apktool. In 14 punti del codice smali viene inserita una chiamata a
una classe Java nuova, [`RadioFix`](patcher/src/com/saicmotor/service/radio/fix/RadioFix.java), aggiunta all'APK come dex
separato. Ogni metodo di `RadioFix` è protetto da `try/catch`: in caso di errore torna al comportamento originale.

| Punto (smali) | Hook |
|---|---|
| `RadioService$6.onProgramInfoChanged` | `shouldCollect()` — ignora le stazioni fuori scansione |
| `RadioTunerExt.scan` / `RadioService.amOrFmScanOver` | `onScanStart()` / `onScanOver()` |
| `RadioService$7.onDabServiceLogoChanged` | ciclo `i < size()` invece di `size() - 1` |
| `RadioService$7.onDabServiceInformationListChanged` | `acceptLogo()` |
| `BitmapFactory.decodeByteArray` (loghi) | `decodeLogo()` — immagine quadrata |
| `List.add` su `mLogoList` / `mServiceInformation` | `addCapped()` |
| `RadioService$7.onDabSlideShowChanged` | `onSlide()` — limite, home, memoria per stazione |
| `RadioService$5.onCurrentProgramChange` | `onStationChanged()` — immagine salvata |
| `AudioController.onAudioFocusChange` | `onFocusChange()` |
| `RadioService.lambda$initRadioListener$0` | `onProgram()` — ricorda se stavi ascoltando |
| `LastMode.tuneDefault` | `resumeState()` |
| `RadioService.onCreate` | `scheduleBootResume()` |

L'APK viene firmato con la **chiave di piattaforma pubblica AOSP** (`test-key`), la stessa usata da SAIC
per questo firmware (impronta SHA-256 `C8:A2:E9:BC:…:2A:B8`).

---

## Avvertenze

Progetto non ufficiale, non affiliato a SAIC Motor o MG. Modifichi il software della tua auto a tuo rischio.
Il codice originale del servizio radio appartiene a SAIC Motor. Il codice sorgente di questo repository contiene solo le
modifiche (script, hook, classe `RadioFix`, licenza MIT); l'APK nella cartella `apk/` include il servizio SAIC modificato.

Grazie a [Skittle6938](https://github.com/Skittle6938/MG4-Radio-DABplus-Fix) per l'idea delle copertine DAB+ sulla
home e della ripresa automatica (firmware LL).
