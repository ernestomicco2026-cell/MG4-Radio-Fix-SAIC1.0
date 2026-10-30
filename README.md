# MG4 Radio Fix — SAIC 1.0 (EH32 EU)

🇮🇹 Italiano · [🇬🇧 English](README.en.md)

[![Offrimi un caffè con PayPal](https://img.shields.io/badge/Offrimi%20un%20caff%C3%A8-PayPal-0070BA?style=for-the-badge&logo=paypal&logoColor=white)](https://paypal.me/ErnestoMicco?locale.x=it_IT&country.x=IT)

Correzioni al **servizio radio originale** dell'infotainment MG4 con sistema **SAIC 1.0**
(allestimenti Comfort, Luxury, Trophy, XPower).

> ⚠️ **Non è per le versioni "Color Radio"** e non è per il firmware **EH32 LL**
> (dove la radio è un'unica app `com.saicmotor.radio`: per quello esiste
> [MG4-Radio-DABplus-Fix](https://github.com/Skittle6938/MG4-Radio-DABplus-Fix)).

> 🛑 **L'APK pronto è SOLO per l'infotainment SWI68-29958-1300R69 e R73** (servizio radio
> `com.saicmotor.service.radio` versione **`1.0.0_EH32_SOP_2502201114`**). La tua versione la trovi
> nelle informazioni di sistema dell'auto.
> La R73 ha il servizio radio identico alla R69, byte per byte.
> È stato **provato solo su MG4 Luxury**. Su altre release (R59, R63, R67, R71…) **non installarlo**:
> costruisci l'APK per la tua versione con lo [script di build](#costruire-lapk-per-la-tua-versione).
> Se l'hai già installato per errore, vedi [Tornare all'originale](#tornare-alloriginale):
> si toglie anche **senza computer**.

---

## Cosa corregge

| Problema | Causa nel firmware originale | Correzione |
|---|---|---|
| **FM bloccato su una sola stazione** (es. resta sulla 90.90 e avanti/indietro non cambiano) | Ogni frequenza ricevuta, anche fuori dalla ricerca (es. dopo un seek), viene messa in una lista che 3 s dopo **sostituisce** l'intera lista stazioni | La lista si aggiorna solo durante una vera scansione |
| **FM che non cambia frequenza quando il segnale cala** (v1.1) | Quando la stazione si sente male il sintonizzatore passa da solo a un'altra frequenza della stessa stazione (AF/RDS), ma il servizio resta su quella vecchia | Il servizio segue il cambio di frequenza (serve **AF attivo** nelle impostazioni della radio) |
| **La radio non riparte all'accensione** | Allo spegnimento la radio perde il focus audio e salva lo stato "in pausa" | Se stavi ascoltando la radio, all'avvio riparte; se l'avevi messa in pausa tu o usavi un'altra sorgente (CarPlay, Bluetooth…) resta ferma |
| **Loghi DAB che non compaiono** | Errore nel ciclo di abbinamento logo/stazione (`size() - 1`): l'ultima stazione ricevuta non viene mai controllata | Ciclo corretto |
| **Loghi DAB scartati** | Accettati solo loghi `logo_unrestricted` perfettamente quadrati | Accettati anche quelli rettangolari, resi quadrati (ritaglio delle bande uniformi o centratura) |
| **Nessuna immagine DAB+ sulla schermata home** | Lo slideshow va solo all'app radio | Le immagini dello slideshow vengono mandate anche alla home |
| **Lista FM piena di frequenze senza stazione** (v1.5) | La ricerca salva ogni frequenza con un po' di segnale (anche 88.70, 88.75, 88.80 della stessa stazione) e il fruscio | Nella lista entrano solo stazioni con nome RDS, una per nome. Le stazioni si aggiungono da sole quando le ascolti; "aggiorna lista" rifà la lista con una verifica RDS a radio muta (1–2 minuti). Ogni 30 minuti, se il sintonizzatore lo supporta, l'elenco di sottofondo aggiunge le stazioni nuove senza interrompere l'audio |
| **App Radio bloccata dopo "cerca" / "aggiorna lista"** (v1.5) | L'app Radio blocca i tocchi finché il servizio non risponde "ricerca finita", ma il servizio non lo fa mai | A fine ricerca il servizio risponde e l'app si sblocca; dopo una ricerca FM la radio torna sulla stazione che stavi ascoltando |
| **Impostazioni (annuncio traffico, frequenza alternativa…) che tornano indietro** (v1.5) | L'interruttore segue solo la conferma del sintonizzatore, che può non arrivare; una ricerca può azzerare la configurazione RDS | Dopo ogni modifica l'app riceve lo stato salvato dal servizio; dopo ogni ricerca la configurazione RDS viene riapplicata |
| **Nomi RDS che non compaiono** (v1.4) | Il nome RDS arriva sulla stazione in ascolto, ma subito dopo il servizio la sostituisce con la voce della lista: se la voce è senza nome (es. dopo una ricerca) si vede solo la frequenza | Il nome RDS viene scritto anche nella voce della lista, che così mostra il nome della stazione |
| **Lista FM ridotta a una sola stazione** (v1.3) | Quando il sintonizzatore avvia una ricerca in sottofondo il firmware cancella tutta la lista FM; se la ricerca non porta risultati resta solo la stazione in ascolto | La lista salvata non viene più cancellata: a fine ricerca viene sostituita solo se la nuova è sensata (non 1 stazione al posto di 29) |
| **Ricerca FM che non parte** (v1.3) | Il comando "cerca" del servizio ignora la banda e fa sempre la ricerca AM | Con la banda FM parte la ricerca FM. Se la lista FM ha 0 o 1 stazioni, la radio fa da sola una ricerca completa (una volta per accensione) |
| **Servizio radio che si blocca a ogni avvio** (v1.2) | Un promemoria DAB salvato senza ora fa andare in errore il servizio a ogni avvio, perché resta salvato. Sui firmware più vecchi si riavvia tutto l'infotainment | I promemoria senza ora valida o già passati non vengono programmati e vengono cancellati |
| **Errori nei thread di lavoro che chiudono il servizio** (v1.2) | Un errore imprevisto in un'operazione in background (database, promemoria…) chiude tutto il servizio radio, anche con il DAB acceso | L'errore viene registrato nel log e il servizio resta attivo |
| **Errore all'avvio se il sintonizzatore non è pronto** (v1.2) | Con l'elenco dei sintonizzatori ancora vuoto il servizio va in errore | Si salta la configurazione e il servizio riprova da solo |
| **Attesa dell'immagine a ogni cambio stazione** | Nessuna memoria dello slideshow | L'ultima immagine di ogni stazione viene salvata e mostrata subito |
| **Memoria che cresce nel tempo** | Slideshow, loghi e info stazioni accumulati senza limite | Ultime 4 immagini, liste con deduplica e limite |

**Non tocca la grafica**: l'app con la schermata della radio (`com.saicmotor.hmi.radio`) resta quella di fabbrica.
**Non migliora la ricezione** (segnale debole, DAB instabile): quella dipende dal sintonizzatore e dal suo firmware.

---

## Compatibilità

| Firmware | Stato |
|---|---|
| SAIC 1.0, SWI68-29958-1300**R69** e **R73**, servizio `1.0.0_EH32_SOP_2502201114` | ✅ [APK pronto nelle Releases](https://github.com/ernestomicco2026-cell/MG4-Radio-Fix-SAIC1.0/releases/latest), **provato su MG4 Luxury** (Comfort, Trophy e XPower non ancora provati) |
| SAIC 1.0, altre release (R59, R63, R67, R71…) | ⚠️ **Non usare l'APK pronto.** Usa lo **script di build** sul tuo APK: verifica da solo se il codice è compatibile |
| EH32 LL (`com.saicmotor.radio`) | ❌ Firmware diverso → [MG4-Radio-DABplus-Fix](https://github.com/Skittle6938/MG4-Radio-DABplus-Fix) |
| Color Radio | ❌ Non supportato |

Controlla la versione del tuo servizio radio:

```bash
adb shell dumpsys package com.saicmotor.service.radio | grep versionName
```

Se è **`1.0.0_EH32_SOP_2502201114`** puoi usare l'APK pronto: **[scaricalo qui](https://github.com/ernestomicco2026-cell/MG4-Radio-Fix-SAIC1.0/releases/latest)** (SHA-256 indicato nella release). Altrimenti costruisci il tuo (sotto).

---

## Installazione

**Non serve un computer.** Basta una chiavetta USB: l'APK si installa dal gestore file dell'auto,
come una normale app.

### Perché si può installare da chiavetta

Il servizio radio è un'app di sistema già presente sull'auto. Questo APK ha **lo stesso nome di pacchetto**
(`com.saicmotor.service.radio`) e **la stessa firma digitale** dell'originale. Per questo Android non lo
tratta come una nuova app, ma come un **aggiornamento** di quella esistente, esattamente come farebbe con un
aggiornamento ufficiale.

In pratica:
- **non** compare una nuova icona: il servizio radio lavora in background e lo usa l'app Radio di sempre;
- **non** cambia la grafica: la schermata della radio resta quella di fabbrica;
- **non** serve root e la partizione di sistema **non** viene toccata;
- l'originale resta intatto in `/system` e si può ripristinare in qualsiasi momento.

### Prima di iniziare: controlla la versione

L'APK pronto è costruito **solo** per il servizio radio **`1.0.0_EH32_SOP_2502201114`**, presente
sull'infotainment **SWI68-29958-1300R69** e **R73** (lo trovi nelle informazioni di sistema dell'auto), ed è stato
provato su **MG4 Luxury**.
**Se la tua versione non è R69 o R73, non installarlo**: la radio potrebbe smettere di funzionare.
Costruisci invece l'APK per la tua versione (vedi [sotto](#costruire-lapk-per-la-tua-versione)).

### Opzione A — Da chiavetta USB (consigliata)

1. Scarica **[RadioService_radiofix6_1.0.0_EH32_SOP_2502201114.apk](https://github.com/ernestomicco2026-cell/MG4-Radio-Fix-SAIC1.0/releases/latest)** dalla sezione **Assets** dell'ultima release.
2. Copialo su una chiavetta USB formattata **FAT32** o **exFAT** (anche nella cartella principale va bene).
3. Con l'auto accesa, inserisci la chiavetta nella porta USB.
4. Apri il **gestore file** dell'infotainment e vai sulla chiavetta.
5. Tocca l'APK: comparirà la richiesta di **aggiornare** un'app esistente. Conferma con **Installa**.
6. Al termine **riavvia l'unità**: tieni premuto il tasto **Home per circa 20 secondi**, finché lo schermo
   si spegne e si riaccende.
7. Apri la radio e **rifai una ricerca FM**, così la lista stazioni torna completa.

> ⚠️ **Il riavvio è obbligatorio.** Il servizio radio resta sempre in memoria: finché non riavvii continua
> a girare la versione vecchia e sembra che l'aggiornamento "non abbia fatto niente".

### Opzione B — Con ADB (da computer)

Serve ADB attivo sull'unità (per l'MG4: [ADB_util di Leon Kerman, thread XDA](https://xdaforums.com/t/mg4-electric-aaos-9-playing-and-possibly-other-mg-models.4697712/post-90591053)).

```bash
adb install -r RadioService_radiofix6_1.0.0_EH32_SOP_2502201114.apk
adb reboot
```

Con ADB puoi anche controllare la versione esatta del servizio prima di installare:

```bash
adb shell dumpsys package com.saicmotor.service.radio | grep versionName
```

### Se qualcosa non va

| Messaggio / situazione | Cosa significa |
|---|---|
| **"App non installata"** o **"conflitto con un pacchetto esistente"** | Il servizio radio della tua auto è firmato con un'altra chiave o è di un firmware diverso (es. LL o Color Radio). Questo APK non è per la tua auto. |
| Il gestore file **non apre** gli APK | Su alcune versioni l'installazione da gestore file è bloccata: usa l'Opzione B. |
| Dopo l'installazione **non cambia nulla** | Non hai riavviato l'unità (punto 6). |
| Dopo l'installazione **la radio non funziona più** | Probabilmente la tua versione non è la R69 o la R73. Togli la patch: vedi [Tornare all'originale](#tornare-alloriginale). |
| La lista FM è **vuota o incompleta** | Rifai una ricerca FM dall'app Radio (punto 7). |

### Tornare all'originale

Ti serve anche se hai installato l'APK su una versione sbagliata (non R69 o R73) e la radio non funziona più.
L'originale di fabbrica è rimasto intatto in `/system`: basta togliere l'aggiornamento e riavviare.

**1. Dalle Impostazioni (senza computer):** Impostazioni dell'auto → elenco delle app → mostra le app di
sistema → servizio radio (`com.saicmotor.service.radio`, a volte "RadioService") → menu ⋮ →
**Disinstalla aggiornamenti**, poi riavvia. Non tutte le versioni del firmware mostrano questa voce:
in quel caso usa il punto 2.

**2. Con Radio Fix Remover, da chiavetta USB (senza computer e senza ADB):**

1. Scarica **`RadioFixRemover_1.0.apk`** dalla sezione **Assets** dell'[ultima release](https://github.com/ernestomicco2026-cell/MG4-Radio-Fix-SAIC1.0/releases/latest)
   e copialo su una chiavetta USB **FAT32** o **exFAT**.
2. Inserisci la chiavetta, apri il **gestore file** dell'auto, tocca l'APK e conferma **Installa**
   (come per la patch).
3. Apri **Radio Fix Remover** e tocca **Remove Radio Fix**. Se Android chiede
   *"Sostituire l'app con la versione di fabbrica?"*, conferma.
4. Quando la schermata dice che è attivo il servizio radio di fabbrica, tocca **Restart head unit**
   (oppure tieni premuto Home per circa 20 secondi).
5. Dopo il riavvio riaprila e tocca **Uninstall this helper app** per toglierla.

L'app è firmata con la stessa chiave di sistema della patch: per questo può rimuovere l'aggiornamento come farebbe
`adb uninstall`. Funziona su qualsiasi versione SAIC 1.0 con servizio radio firmato AOSP, non solo sulla R69.
Il codice è in [`remover/`](remover/).

**3. Con ADB:**

```bash
adb uninstall com.saicmotor.service.radio
adb reboot
```

In tutti i casi torna automaticamente la versione di fabbrica, che è rimasta intatta in `/system`.

**Ultima spiaggia:** il ripristino dei dati di fabbrica dell'infotainment toglie anche la patch, ma cancella
impostazioni, profili, telefoni abbinati e le altre app installate.

> ⚠️ Non installare sopra l'APK originale preso da un'altra auto: se la versione è diversa dalla tua
> avresti di nuovo un servizio radio che non corrisponde al tuo sistema.

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
(una stazione che prima avrebbe "bloccato" la lista), `scan list FM … scartata` (lista troppo corta non salvata), `ricerca FM`, `AF follow` (cambio di frequenza seguito), `auto resume`, `restored art`.

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

## Come vengono create le release

Le release sono **costruite e firmate automaticamente da GitHub Actions**
([`.github/workflows/release.yml`](.github/workflows/release.yml)) a partire dal codice di questo repository:
ogni APK pubblicato corrisponde esattamente ai sorgenti visibili qui.

Per costruire serve l'APK originale del servizio radio. Nel repository è presente solo **cifrato**
(`firmware/*.apk.enc`, AES-256): viene decifrato solo durante la build, con una chiave conservata nei
*Secrets* privati del repository, e non viene mai pubblicato in chiaro.

---

## Sostieni il progetto

MG4 Radio Fix è e resterà **gratuito**. Se ti è utile e ti va di contribuire alle ore passate a svilupparlo e provarlo
in auto, puoi lasciare un'offerta: è del tutto **volontaria** e **non c'è un importo minimo**; anche una stella al repository o una
segnalazione di un problema sono un aiuto prezioso. Grazie!

Anche solo **2 €** sono un aiuto prezioso per continuare a sviluppare e provare in auto le nuove versioni.

[![Offrimi un caffè con PayPal](https://img.shields.io/badge/Offrimi%20un%20caff%C3%A8-PayPal-0070BA?style=for-the-badge&logo=paypal&logoColor=white)](https://paypal.me/ErnestoMicco?locale.x=it_IT&country.x=IT)

---

## Avvertenze

Progetto non ufficiale, non affiliato a SAIC Motor o MG. Modifichi il software della tua auto a tuo rischio.
Il codice originale del servizio radio appartiene a SAIC Motor. Il codice sorgente di questo repository contiene solo le
modifiche (script, hook, classe `RadioFix`, licenza MIT); l'APK nelle Releases include il servizio SAIC modificato.

Grazie a [Skittle6938](https://github.com/Skittle6938/MG4-Radio-DABplus-Fix) per l'idea delle copertine DAB+ sulla
home e della ripresa automatica (firmware LL).
