# MG4 Radio Fix — SAIC 1.0 (EH32 EU)

<p align="center">
<b>Correzioni al servizio radio originale dell'infotainment MG4 SAIC 1.0</b><br>
<i>Fixes for the original radio service of the MG4 SAIC 1.0 infotainment</i>
</p>

<p align="center">
<a href="https://github.com/ernestomicco2026-cell/MG4-Radio-Fix-SAIC1.0/releases/latest"><img alt="Scarica / Download 1.5" src="https://img.shields.io/badge/Scarica%20%2F%20Download-1.5-2ea44f?style=for-the-badge&logo=android&logoColor=white"></a>
<a href="https://paypal.me/ErnestoMicco?locale.x=it_IT&country.x=IT"><img alt="Sostieni il progetto con PayPal · Support the project with PayPal" src="https://img.shields.io/badge/Sostieni%20%2F%20Support-PayPal-0070BA?style=for-the-badge&logo=paypal&logoColor=white"></a>
</p>

<p align="center">
🇮🇹 <a href="#italiano">Italiano</a> · 🇬🇧 <a href="#english">English</a> ·
<a href="#quale-versione-installare--which-version-should-i-install">Quale versione? / Which version?</a> ·
<a href="#versioni">Novità</a> / <a href="#changelog">What's new</a>
</p>

> 🛑 **L'APK pronto è SOLO per l'infotainment SWI68-29958-1300R69 e R73** (servizio radio
> `com.saicmotor.service.radio` versione **`1.0.0_EH32_SOP_2502201114`**). La tua versione la trovi
> nelle informazioni di sistema dell'auto.
> La R73 ha il servizio radio identico alla R69, byte per byte.
> È stato **provato solo su MG4 Luxury**. Su altre release (R59, R63, R67, R71…) **non installarlo**:
> costruisci l'APK per la tua versione con lo [script di build](#costruire-lapk-per-la-tua-versione).
> Se l'hai già installato per errore, vedi [Tornare all'originale](#tornare-alloriginale):
> si toglie anche **senza computer**.
>
> 🛑 **The ready-made APK is ONLY for infotainment SWI68-29958-1300R69 and R73** (radio service
> `com.saicmotor.service.radio` version **`1.0.0_EH32_SOP_2502201114`**). You can find your version in
> the car's system information.
> R73 has a radio service identical to R69, byte for byte.
> It has **only been tested on an MG4 Luxury**. On other releases (R59, R63, R67, R71…) **do not install it**:
> build the APK for your own version with the [build script](#building-the-apk-for-your-version).
> If you already installed it by mistake, see [Going back to the original](#going-back-to-the-original):
> it can be removed **without a computer**.

> ⚠️ **Non è per le versioni "Color Radio"** e non è per il firmware **EH32 LL**
> (dove la radio è un'unica app `com.saicmotor.radio`: per quello esiste
> [MG4-Radio-DABplus-Fix](https://github.com/Skittle6938/MG4-Radio-DABplus-Fix)).
>
> ⚠️ **Not for "Color Radio" versions** and not for the **EH32 LL** firmware
> (where the radio is a single app, `com.saicmotor.radio`: for that one use
> [MG4-Radio-DABplus-Fix](https://github.com/Skittle6938/MG4-Radio-DABplus-Fix)).

---

## Quale versione installare? / Which version should I install?

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

| Firmware | Status |
|---|---|
| SAIC 1.0, SWI68-29958-1300**R69** and **R73**, service `1.0.0_EH32_SOP_2502201114` | ✅ [Ready-made APK in Releases](https://github.com/ernestomicco2026-cell/MG4-Radio-Fix-SAIC1.0/releases/latest), **tested on MG4 Luxury** (Comfort, Trophy and XPower not tested yet) |
| SAIC 1.0, other releases (R59, R63, R67, R71…) | ⚠️ **Do not use the ready-made APK.** Use the **build script** on your own APK: it checks by itself whether the code is compatible |
| EH32 LL (`com.saicmotor.radio`) | ❌ Different firmware → [MG4-Radio-DABplus-Fix](https://github.com/Skittle6938/MG4-Radio-DABplus-Fix) |
| Color Radio | ❌ Not supported |

Check the version of your radio service:

```bash
adb shell dumpsys package com.saicmotor.service.radio | grep versionName
```

If it is **`1.0.0_EH32_SOP_2502201114`** you can use the ready-made APK: **[download it here](https://github.com/ernestomicco2026-cell/MG4-Radio-Fix-SAIC1.0/releases/latest)** (SHA-256 listed in the release). Otherwise build your own (below).

---

## Italiano

Correzioni al **servizio radio originale** dell'infotainment MG4 con sistema **SAIC 1.0**
(allestimenti Comfort, Luxury, Trophy, XPower).

**Indice:** [Cosa corregge](#cosa-corregge) · [Installazione](#installazione) · [Aggiornamenti](#aggiornamenti) · [MG4 Store](#mg4-store) · [Tornare all'originale](#tornare-alloriginale) · [Comandi utili](#comandi-utili) · [Costruire l'APK](#costruire-lapk-per-la-tua-versione) · [Segnalare un problema](#segnalare-un-problema) · [Sostieni il progetto](#sostieni-il-progetto) · [Versioni](#versioni) · [Cosa arriva](#cosa-arriva)

### Cosa corregge

| Problema | Causa nel firmware originale | Correzione |
|---|---|---|
| **FM bloccato su una sola stazione** (es. resta sulla 90.90 e avanti/indietro non cambiano) | Ogni frequenza ricevuta, anche fuori dalla ricerca (es. dopo un seek), viene messa in una lista che 3 s dopo **sostituisce** l'intera lista stazioni | La lista si aggiorna solo durante una vera scansione |
| **FM che non cambia frequenza quando il segnale cala** (v1.1) | Quando la stazione si sente male il sintonizzatore passa da solo a un'altra frequenza della stessa stazione (AF/RDS), ma il servizio resta su quella vecchia | Il servizio segue il cambio di frequenza (serve **AF attivo** nelle impostazioni della radio) |
| **La radio non riparte all'accensione** | Allo spegnimento la radio perde il focus audio e salva lo stato "in pausa" | Se stavi ascoltando la radio, all'avvio riparte; se l'avevi messa in pausa tu o usavi un'altra sorgente (CarPlay, Bluetooth…) resta ferma |
| **Loghi DAB che non compaiono** | Errore nel ciclo di abbinamento logo/stazione (`size() - 1`): l'ultima stazione ricevuta non viene mai controllata | Ciclo corretto |
| **Loghi DAB scartati** | Accettati solo loghi `logo_unrestricted` perfettamente quadrati | Accettati anche quelli rettangolari, resi quadrati (ritaglio delle bande uniformi o centratura) |
| **Nessuna immagine DAB+ sulla schermata home** | Lo slideshow va solo all'app radio | Le immagini dello slideshow vengono mandate anche alla home |
| **FM che resta su una frequenza che si sente male** (v1.6, in prova) | Il cambio automatico di frequenza (AF) veniva scartato se arrivava subito dopo aver scelto la stazione. In più il sintonizzatore di solito cambia frequenza solo quando il segnale si perde del tutto | Il cambio di frequenza della stessa stazione (stesso codice RDS) viene seguito sempre. Se il segnale resta debole per alcuni secondi, la radio prova da sola un'altra frequenza già nota della stessa stazione e ci passa solo se si sente nettamente meglio, con una pausa dell'audio di circa un secondo. La lista ricorda la frequenza buona, e tornando a casa la radio torna indietro allo stesso modo. Serve **AF attivo** nelle impostazioni della radio |
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

### Installazione

**Non serve un computer.** Basta una chiavetta USB: l'APK si installa dal gestore file dell'auto,
come una normale app.

#### Perché si può installare da chiavetta

Il servizio radio è un'app di sistema già presente sull'auto. Questo APK ha **lo stesso nome di pacchetto**
(`com.saicmotor.service.radio`) e **la stessa firma digitale** dell'originale. Per questo Android non lo
tratta come una nuova app, ma come un **aggiornamento** di quella esistente, esattamente come farebbe con un
aggiornamento ufficiale.

In pratica:
- **non** compare una nuova icona: il servizio radio lavora in background e lo usa l'app Radio di sempre;
- **non** cambia la grafica: la schermata della radio resta quella di fabbrica;
- **non** serve root e la partizione di sistema **non** viene toccata;
- l'originale resta intatto in `/system` e si può ripristinare in qualsiasi momento.

#### Prima di iniziare: controlla la versione

L'APK pronto è costruito **solo** per il servizio radio **`1.0.0_EH32_SOP_2502201114`**, presente
sull'infotainment **SWI68-29958-1300R69** e **R73** (lo trovi nelle informazioni di sistema dell'auto), ed è stato
provato su **MG4 Luxury**.
**Se la tua versione non è R69 o R73, non installarlo**: la radio potrebbe smettere di funzionare.
Costruisci invece l'APK per la tua versione (vedi [sotto](#costruire-lapk-per-la-tua-versione)).

#### Opzione A — Da chiavetta USB (consigliata)

1. Scarica **[RadioService_radiofix7_1.0.0_EH32_SOP_2502201114.apk](https://github.com/ernestomicco2026-cell/MG4-Radio-Fix-SAIC1.0/releases/latest)** dalla sezione **Assets** dell'ultima release.
2. Copialo su una chiavetta USB formattata **FAT32** o **exFAT** (anche nella cartella principale va bene).
3. Con l'auto accesa, inserisci la chiavetta nella porta USB.
4. Apri il **gestore file** dell'infotainment e vai sulla chiavetta.
5. Tocca l'APK: comparirà la richiesta di **aggiornare** un'app esistente. Conferma con **Installa**.
6. Al termine **riavvia l'unità**: tieni premuto il tasto **Home per circa 20 secondi**, finché lo schermo
   si spegne e si riaccende.
7. Apri la radio e **rifai una ricerca FM**, così la lista stazioni torna completa.

> ⚠️ **Il riavvio è obbligatorio.** Il servizio radio resta sempre in memoria: finché non riavvii continua
> a girare la versione vecchia e sembra che l'aggiornamento "non abbia fatto niente".

#### Opzione B — Con ADB (da computer)

Serve ADB attivo sull'unità (per l'MG4: [ADB_util di Leon Kerman, thread XDA](https://xdaforums.com/t/mg4-electric-aaos-9-playing-and-possibly-other-mg-models.4697712/post-90591053)).

```bash
adb install -r RadioService_radiofix7_1.0.0_EH32_SOP_2502201114.apk
adb reboot
```

Con ADB puoi anche controllare la versione esatta del servizio prima di installare:

```bash
adb shell dumpsys package com.saicmotor.service.radio | grep versionName
```

#### Se qualcosa non va

| Messaggio / situazione | Cosa significa |
|---|---|
| **"App non installata"** o **"conflitto con un pacchetto esistente"** | Il servizio radio della tua auto è firmato con un'altra chiave o è di un firmware diverso (es. LL o Color Radio). Questo APK non è per la tua auto. |
| Il gestore file **non apre** gli APK | Su alcune versioni l'installazione da gestore file è bloccata: usa l'Opzione B. |
| Dopo l'installazione **non cambia nulla** | Non hai riavviato l'unità (punto 6). |
| Dopo l'installazione **la radio non funziona più** | Probabilmente la tua versione non è la R69 o la R73. Togli la patch: vedi [Tornare all'originale](#tornare-alloriginale). |
| La lista FM è **vuota o incompleta** | Rifai una ricerca FM dall'app Radio (punto 7). |

### Aggiornamenti

La Radio Fix non si aggiorna da sola. Quando esce una versione nuova, scarica l'APK dall'[ultima release](https://github.com/ernestomicco2026-cell/MG4-Radio-Fix-SAIC1.0/releases/latest)
e installalo sopra quello che hai, nello stesso modo dell'installazione (chiavetta o adb). Poi **riavvia l'unità**
(Home premuto per circa 20 secondi) e, se la lista FM è vuota o incompleta, rifai una ricerca FM.

### MG4 Store

Ora c'è **MG4 Store** per scaricare e aggiornare tutte le nostre app MG4 (launcher, MG4 Clima, MG4 Task Manager,
MG4 Browser, MG4 Games e quelle che arriveranno): si trova anche nel launcher MG4 (icona **Store**).
Repository: [ernestomicco2026-cell/store](https://github.com/ernestomicco2026-cell/store).
**La Radio Fix non è nello Store:** corregge il servizio radio di sistema e va scelta in base alla versione dell'auto,
quindi si installa a mano, come spiegato sopra.

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

### Comandi utili

```bash
# versione del servizio radio installato
adb shell dumpsys package com.saicmotor.service.radio | grep versionName

# vedere i messaggi della Radio Fix (Ctrl+C per fermare)
adb logcat -s RadioFix
```

Messaggi tipici: `scan start` / `scan over` (ricerca FM), `ignored program info outside scan`
(una stazione che prima avrebbe "bloccato" la lista), `scan list FM … scartata` (lista troppo corta non salvata), `ricerca FM`, `AF follow` (cambio di frequenza seguito), `auto resume`, `restored art`.

### Costruire l'APK per la tua versione

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

### Come funziona (tecnico)

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

### Come vengono create le release

Le release sono **costruite e firmate automaticamente da GitHub Actions**
([`.github/workflows/release.yml`](.github/workflows/release.yml)) a partire dal codice di questo repository:
ogni APK pubblicato corrisponde esattamente ai sorgenti visibili qui.

Per costruire serve l'APK originale del servizio radio. Nel repository è presente solo **cifrato**
(`firmware/*.apk.enc`, AES-256): viene decifrato solo durante la build, con una chiave conservata nei
*Secrets* privati del repository, e non viene mai pubblicato in chiaro.

### Segnalare un problema

Le segnalazioni si aprono nella sezione [Issues](../../issues). Scrivi la versione del software (per esempio
SWI68-29958-1300R69), la versione del servizio radio (il comando in [Comandi utili](#comandi-utili)) e cosa succede.
Se usi **MG4 Launcher SWI68**, il launcher raccoglie nei suoi log anche le righe `RadioFix`: basta dirlo nella
segnalazione. Se lo script di build si ferma, allega il tuo APK originale.

### Sostieni il progetto

MG4 Radio Fix è e resterà **gratuito**. Se ti è utile e ti va di contribuire alle ore passate a svilupparlo e provarlo
in auto, puoi lasciare un'offerta: è del tutto **volontaria** e **non c'è un importo minimo**; anche una stella al repository o una
segnalazione di un problema sono un aiuto prezioso. Grazie!

Anche solo **2 €** sono un aiuto prezioso per continuare a sviluppare e provare in auto le nuove versioni.

[![Offrimi un caffè con PayPal](https://img.shields.io/badge/Offrimi%20un%20caff%C3%A8-PayPal-0070BA?style=for-the-badge&logo=paypal&logoColor=white)](https://paypal.me/ErnestoMicco?locale.x=it_IT&country.x=IT)

### Versioni

#### 1.5
- **Lista FM pulita:** entrano solo stazioni con nome RDS, una per nome; le stazioni si aggiungono da sole quando le
  ascolti e "aggiorna lista" rifà la lista con una verifica RDS.
- **App Radio non più bloccata** dopo "cerca" o "aggiorna lista"; dopo una ricerca FM torna la stazione che ascoltavi.
- **Impostazioni** (annuncio traffico, frequenza alternativa…) che non tornano più indietro.

#### 1.4
- **Nomi RDS** visibili anche nella lista delle stazioni.

#### 1.3
- La **lista FM** non si riduce più a una sola stazione dopo una ricerca in sottofondo.
- La **ricerca FM** parte davvero (prima faceva sempre la ricerca AM); con lista vuota la radio la fa da sola.

#### 1.2
- Il servizio radio **non si blocca più a ogni avvio** per un promemoria DAB senza ora.
- Un errore in un'operazione in background non chiude più tutto il servizio; nessun errore se il sintonizzatore non è
  ancora pronto.
- Disponibile anche per la **R73**, che ha il servizio radio identico alla R69.

#### 1.1
- La radio **segue il cambio di frequenza** della stessa stazione quando il segnale cala (serve AF attivo).
- **Radio Fix Remover** per togliere la patch senza computer.

#### 1.0
- Prima versione: FM non più bloccato su una stazione, ripresa della radio all'accensione, loghi DAB corretti, immagini
  DAB+ anche sulla home, memoria dello slideshow, memoria che non cresce più.

### Cosa arriva

- **Passo 1 — versione 1.6 (in prova):** la radio non resta su una frequenza che si sente male: segue sempre il cambio
  di frequenza della stessa stazione e, se il segnale resta debole, prova da sola un'altra frequenza già nota (serve
  **AF attivo**). Si pubblica dopo le prove in auto.
- **Più avanti:** APK pronti per altre versioni del firmware, quando arrivano i servizi radio originali da provare.

Molte idee arrivano dalle segnalazioni (issue) degli utenti: grazie!

### Avvertenze

Progetto non ufficiale, non affiliato a SAIC Motor o MG. Modifichi il software della tua auto a tuo rischio.
Il codice originale del servizio radio appartiene a SAIC Motor. Il codice sorgente di questo repository contiene solo le
modifiche (script, hook, classe `RadioFix`, licenza MIT); l'APK nelle Releases include il servizio SAIC modificato.

Grazie a [Skittle6938](https://github.com/Skittle6938/MG4-Radio-DABplus-Fix) per l'idea delle copertine DAB+ sulla
home e della ripresa automatica (firmware LL).

---

## English

Fixes for the **original radio service** of the MG4 infotainment with the **SAIC 1.0** system
(Comfort, Luxury, Trophy, XPower trims).

**Contents:** [What it fixes](#what-it-fixes) · [Installation](#installation) · [Updates](#updates) · [MG4 Store](#mg4-store-1) · [Going back to the original](#going-back-to-the-original) · [Useful commands](#useful-commands) · [Building the APK](#building-the-apk-for-your-version) · [Reporting a problem](#reporting-a-problem) · [Support the project](#support-the-project) · [Changelog](#changelog) · [What's coming](#whats-coming)

### What it fixes

| Problem | Cause in the original firmware | Fix |
|---|---|---|
| **FM stuck on a single station** (e.g. stays on 90.90 and next/previous do nothing) | Every received frequency, even outside a search (e.g. after a seek), is put into a list that **replaces** the whole station list 3 s later | The list is only updated during a real scan |
| **FM not changing frequency when the signal fades** (v1.1) | When reception gets weak the tuner switches by itself to another frequency of the same station (AF/RDS), but the service stays on the old one | The service follows the frequency change (requires **AF enabled** in the radio settings) |
| **Radio doesn't resume when the car starts** | When the car turns off, the radio loses audio focus and saves a "paused" state | If you were listening to the radio, it resumes at start-up; if you had paused it yourself or were using another source (CarPlay, Bluetooth…), it stays off |
| **DAB logos not showing** | Bug in the logo/station matching loop (`size() - 1`): the last received station is never checked | Loop fixed |
| **DAB logos discarded** | Only perfectly square `logo_unrestricted` logos are accepted | Rectangular logos are accepted too and made square (uniform borders cropped, or centred) |
| **No DAB+ images on the home screen** | The slideshow only goes to the radio app | Slideshow images are also sent to the home screen |
| **FM staying on a frequency with bad reception** (v1.6, in testing) | The automatic frequency change (AF) was discarded when it came right after choosing the station. Also, the tuner usually changes frequency only when the signal is lost completely | A frequency change of the same station (same RDS code) is always followed. If the signal stays weak for a few seconds, the radio tries another known frequency of the same station by itself and switches only if it is clearly better, with an audio pause of about one second. The list remembers the good frequency, and back home the radio switches back the same way. Requires **AF enabled** in the radio settings |
| **FM list full of frequencies without a station** (v1.5) | The search saves every frequency with a bit of signal (even 88.70, 88.75, 88.80 of the same station) and noise | Only stations with an RDS name get into the list, one per name. Stations are added automatically when you listen to them; "refresh list" rebuilds the list with an RDS check while muted (1–2 minutes). Every 30 minutes, if the tuner supports it, the background program list adds new stations without interrupting the audio |
| **Radio app locked after "search" / "refresh list"** (v1.5) | The Radio app blocks touches until the service answers "search finished", but the service never does | At the end of the search the service answers and the app is unlocked; after an FM search the radio goes back to the station you were listening to |
| **Settings (traffic announcements, alternative frequency…) switching back** (v1.5) | The switch only follows the tuner's confirmation, which may not arrive; a search can reset the RDS configuration | After every change the app receives the state saved by the service; after every search the RDS configuration is applied again |
| **RDS names not showing** (v1.4) | The RDS name reaches the current station, but right after that the service replaces it with the list entry: if the entry has no name (e.g. after a search) only the frequency is shown | The RDS name is also written into the list entry, so the station name is shown |
| **FM list reduced to a single station** (v1.3) | When the tuner starts a background search, the firmware deletes the whole FM list; if the search finds nothing, only the current station is left | The saved list is no longer deleted: at the end of a search it is replaced only if the new one makes sense (not 1 station instead of 29) |
| **FM search not starting** (v1.3) | The service's "search" command ignores the band and always runs an AM search | With the FM band an FM search starts. If the FM list has 0 or 1 stations, the radio runs a full search by itself (once per start-up) |
| **Radio service crashing at every start-up** (v1.2) | A DAB reminder saved without a time makes the service fail at every start-up, because it stays saved. On older firmware the whole infotainment restarts | Reminders without a valid time, or already past, are not scheduled and are deleted |
| **Errors in worker threads closing the service** (v1.2) | An unexpected error in a background task (database, reminders…) closes the whole radio service, even with DAB playing | The error is logged and the service stays alive |
| **Error at start-up when the tuner isn't ready** (v1.2) | With the tuner list still empty the service fails | Configuration is skipped and the service retries by itself |
| **Waiting for the image at every station change** | No slideshow memory | The last image of each station is saved and shown immediately |
| **Memory growing over time** | Slideshow, logos and station info pile up without limit | Last 4 images kept, lists de-duplicated and capped |

**The graphics don't change**: the app with the radio screen (`com.saicmotor.hmi.radio`) stays the factory one.
**Reception doesn't improve** (weak signal, unstable DAB): that depends on the tuner and its firmware.

### Installation

**No computer needed.** A USB stick is enough: the APK is installed from the car's file manager,
like a normal app.

#### Why it can be installed from a USB stick

The radio service is a system app that is already on the car. This APK has **the same package name**
(`com.saicmotor.service.radio`) and **the same digital signature** as the original. That's why Android
doesn't treat it as a new app but as an **update** of the existing one, exactly as it would with an
official update.

In practice:
- **no** new icon appears: the radio service runs in the background and is used by the usual Radio app;
- the graphics **don't** change: the radio screen stays the factory one;
- **no** root needed, and the system partition is **not** touched;
- the original stays intact in `/system` and can be restored at any time.

#### Before you start: check the version

The ready-made APK is built **only** for radio service **`1.0.0_EH32_SOP_2502201114`**, found on
infotainment **SWI68-29958-1300R69** and **R73** (you can see it in the car's system information), and it has been
tested on an **MG4 Luxury**.
**If your version is not R69 or R73, do not install it**: the radio may stop working.
Build the APK for your version instead (see [below](#building-the-apk-for-your-version)).

#### Option A — From a USB stick (recommended)

1. Download **[RadioService_radiofix7_1.0.0_EH32_SOP_2502201114.apk](https://github.com/ernestomicco2026-cell/MG4-Radio-Fix-SAIC1.0/releases/latest)** from the **Assets** section of the latest release.
2. Copy it to a USB stick formatted as **FAT32** or **exFAT** (the root folder is fine).
3. With the car on, plug the stick into the USB port.
4. Open the infotainment **file manager** and go to the USB stick.
5. Tap the APK: you'll be asked to **update** an existing app. Confirm with **Install**.
6. When it's done, **restart the unit**: hold the **Home button for about 20 seconds** until the screen
   turns off and on again.
7. Open the radio and **run a new FM scan**, so the station list is complete again.

> ⚠️ **The restart is mandatory.** The radio service is always in memory: until you restart, the old
> version keeps running and it looks like the update "did nothing".

#### Option B — With ADB (from a computer)

You need ADB enabled on the unit (for the MG4: [ADB_util by Leon Kerman, XDA thread](https://xdaforums.com/t/mg4-electric-aaos-9-playing-and-possibly-other-mg-models.4697712/post-90591053)).

```bash
adb install -r RadioService_radiofix7_1.0.0_EH32_SOP_2502201114.apk
adb reboot
```

With ADB you can also check the exact service version before installing:

```bash
adb shell dumpsys package com.saicmotor.service.radio | grep versionName
```

#### If something goes wrong

| Message / situation | What it means |
|---|---|
| **"App not installed"** or **"conflicts with an existing package"** | Your car's radio service is signed with a different key or is from a different firmware (e.g. LL or Color Radio). This APK is not for your car. |
| The file manager **won't open** APKs | On some versions installing from the file manager is blocked: use Option B. |
| **Nothing changes** after installing | You didn't restart the unit (step 6). |
| **The radio stops working** after installing | Your version is probably not R69 or R73. Remove the patch: see [Going back to the original](#going-back-to-the-original). |
| The FM list is **empty or incomplete** | Run a new FM scan from the Radio app (step 7). |

### Updates

Radio Fix doesn't update itself. When a new version is out, download the APK from the [latest release](https://github.com/ernestomicco2026-cell/MG4-Radio-Fix-SAIC1.0/releases/latest)
and install it over the one you have, the same way as the installation (USB stick or adb). Then **restart the head
unit** (hold Home for about 20 seconds) and, if the FM list is empty or incomplete, run an FM search again.

### MG4 Store

There is now **MG4 Store** to download and update all our MG4 apps (launcher, MG4 Clima, MG4 Task Manager, MG4 Browser,
MG4 Games and the ones still to come): you also find it in the MG4 launcher (**Store** icon).
Repository: [ernestomicco2026-cell/store](https://github.com/ernestomicco2026-cell/store).
**Radio Fix is not in the Store:** it patches the system radio service and must match the car's version, so it is
installed by hand, as explained above.

### Going back to the original

You also need this if you installed the APK on the wrong version (not R69 or R73) and the radio no longer works.
The factory original is still intact in `/system`: you just need to remove the update and restart.

**1. From Settings (no computer):** car Settings → app list → show system apps → radio service
(`com.saicmotor.service.radio`, sometimes "RadioService") → ⋮ menu → **Uninstall updates**, then
restart. Not every firmware version shows this option: if yours doesn't, use option 2.

**2. With Radio Fix Remover, from a USB stick (no computer, no ADB):**

1. Download **`RadioFixRemover_1.0.apk`** from the **Assets** section of the [latest release](https://github.com/ernestomicco2026-cell/MG4-Radio-Fix-SAIC1.0/releases/latest)
   and copy it to a **FAT32** or **exFAT** USB stick.
2. Plug in the stick, open the car's **file manager**, tap the APK and confirm **Install**
   (same as for the patch).
3. Open **Radio Fix Remover** and tap **Remove Radio Fix**. If Android asks
   *"Replace this app with the factory version?"*, confirm.
4. When the screen says the factory radio service is active, tap **Restart head unit**
   (or hold Home for about 20 seconds).
5. After the restart, open it again and tap **Uninstall this helper app** to remove it.

The app is signed with the same system key as the patch, which is why it can remove the update just like
`adb uninstall`. It works on any SAIC 1.0 version with an AOSP-signed radio service, not just R69.
The code is in [`remover/`](remover/).

**3. With ADB:**

```bash
adb uninstall com.saicmotor.service.radio
adb reboot
```

In every case the factory version comes back automatically, since it is still intact in `/system`.

**Last resort:** a factory reset of the infotainment also removes the patch, but it erases settings,
profiles, paired phones and any other installed apps.

> ⚠️ Don't install an original APK taken from another car over it: if its version differs from yours,
> you'd again have a radio service that doesn't match your system.

### Useful commands

```bash
# version of the installed radio service
adb shell dumpsys package com.saicmotor.service.radio | grep versionName

# watch Radio Fix's messages (Ctrl+C to stop)
adb logcat -s RadioFix
```

Typical messages: `scan start` / `scan over` (FM scan), `ignored program info outside scan`
(a station that would previously have "locked" the list), `scan list FM … scartata` (list too short, not saved), `ricerca FM` (FM search), `AF follow` (frequency change followed), `auto resume`, `restored art`.

### Building the APK for your version

Requirements (Linux/macOS/WSL): Java 11+, git, curl, unzip, zip, python3, openssl.

```bash
# 1. pull the original radio service from your car
adb shell pm path com.saicmotor.service.radio
adb pull /system/priv-app/<folder>/<file>.apk RadioService_original.apk   # use the path shown above

# 2. build
cd patcher
./build.sh ../RadioService_original.apk
```

The script downloads the tools it needs, then **stops without producing anything** if:
- the APK is not the EH32 radio service (`com.saicmotor.service.radio`);
- the original is not signed with the AOSP platform key (the update would not install);
- your firmware's code differs from what's expected (one of the patch points can't be found).

If it stops at the third point, open an *issue* attaching your original APK and your SWI68 version.

### How it works (technical)

The original service is decompiled with apktool. A call to a new Java class,
[`RadioFix`](patcher/src/com/saicmotor/service/radio/fix/RadioFix.java), is inserted at 14 points of the smali
code; the class is added to the APK as a separate dex. Every `RadioFix` method is wrapped in `try/catch`:
on error it falls back to the original behaviour.

| Point (smali) | Hook |
|---|---|
| `RadioService$6.onProgramInfoChanged` | `shouldCollect()` — ignores stations outside a scan |
| `RadioTunerExt.scan` / `RadioService.amOrFmScanOver` | `onScanStart()` / `onScanOver()` |
| `RadioService$7.onDabServiceLogoChanged` | loop `i < size()` instead of `size() - 1` |
| `RadioService$7.onDabServiceInformationListChanged` | `acceptLogo()` |
| `BitmapFactory.decodeByteArray` (logos) | `decodeLogo()` — square image |
| `List.add` on `mLogoList` / `mServiceInformation` | `addCapped()` |
| `RadioService$7.onDabSlideShowChanged` | `onSlide()` — limit, home screen, per-station memory |
| `RadioService$5.onCurrentProgramChange` | `onStationChanged()` — saved image |
| `AudioController.onAudioFocusChange` | `onFocusChange()` |
| `RadioService.lambda$initRadioListener$0` | `onProgram()` — remembers whether you were listening |
| `LastMode.tuneDefault` | `resumeState()` |
| `RadioService.onCreate` | `scheduleBootResume()` |

The APK is signed with the **public AOSP platform key** (`test-key`), the same one SAIC used
for this firmware (SHA-256 fingerprint `C8:A2:E9:BC:…:2A:B8`).

### How releases are made

Releases are **built and signed automatically by GitHub Actions**
([`.github/workflows/release.yml`](.github/workflows/release.yml)) from the code in this repository:
every published APK matches exactly the sources you see here.

Building requires the original radio service APK. The repository only contains it **encrypted**
(`firmware/*.apk.enc`, AES-256): it is decrypted only during the build, with a key kept in the repository's
private *Secrets*, and is never published in clear.

### Reporting a problem

Reports go in the [Issues](../../issues) section. Write the software version (for example SWI68-29958-1300R69), the
radio service version (the command in [Useful commands](#useful-commands)) and what happens. If you use
**MG4 Launcher SWI68**, the launcher also collects the `RadioFix` lines in its logs: just mention it in the report.
If the build script stops, attach your original APK.

### Support the project

MG4 Radio Fix is and will stay **free**. If you find it useful and would like to contribute to the hours spent
developing it and testing it in the car, you can leave a donation: it's completely **voluntary** and **there is no minimum amount**; a star on
the repository or a bug report help a lot too. Thank you!

Even just **€2** is a great help to keep developing and testing new versions in the car.

[![Buy me a coffee with PayPal](https://img.shields.io/badge/Buy%20me%20a%20coffee-PayPal-0070BA?style=for-the-badge&logo=paypal&logoColor=white)](https://paypal.me/ErnestoMicco?locale.x=it_IT&country.x=IT)

### Changelog

#### 1.5
- **Clean FM list:** only stations with an RDS name, one per name; stations are added by themselves when you listen to
  them and "refresh list" rebuilds the list with an RDS check.
- **Radio app no longer stuck** after "search" or "refresh list"; after an FM search you're back on your station.
- **Settings** (traffic announcement, alternative frequency…) no longer flip back.

#### 1.4
- **RDS names** shown in the station list too.

#### 1.3
- The **FM list** no longer shrinks to a single station after a background search.
- The **FM search** really starts (it always ran the AM search before); with an empty list the radio runs it by itself.

#### 1.2
- The radio service **no longer crashes at every start** because of a DAB reminder without a time.
- An error in a background task no longer shuts the whole service down; no error if the tuner isn't ready yet.
- Also available for **R73**, whose radio service is identical to R69.

#### 1.1
- The radio **follows the frequency change** of the same station when the signal drops (AF must be on).
- **Radio Fix Remover** to remove the patch without a computer.

#### 1.0
- First version: FM no longer stuck on one station, radio resumes at power-on, DAB logos fixed, DAB+ pictures on the
  home screen too, slideshow memory, memory that no longer grows.

### What's coming

- **Step 1 — version 1.6 (in testing):** the radio no longer stays on a frequency that sounds bad: it always follows
  the frequency change of the same station and, if the signal stays weak, tries another known frequency by itself
  (**AF** must be on). It will be published after the tests in the car.
- **Later:** ready-made APKs for other firmware versions, as original radio services arrive to test.

Many ideas come from users' reports (issues): thank you!

### Disclaimer

Unofficial project, not affiliated with SAIC Motor or MG. You modify your car's software at your own risk.
The original radio service code belongs to SAIC Motor. The source code in this repository contains only the
changes (scripts, hooks, `RadioFix` class, MIT licence); the APK in Releases includes the modified SAIC service.

Thanks to [Skittle6938](https://github.com/Skittle6938/MG4-Radio-DABplus-Fix) for the idea of DAB+ artwork on the
home screen and of automatic resume (LL firmware).
