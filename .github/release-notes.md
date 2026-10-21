[![Offrimi un caffè con PayPal](https://img.shields.io/badge/Offrimi%20un%20caff%C3%A8-PayPal-0070BA?style=for-the-badge&logo=paypal&logoColor=white)](https://www.paypal.com/pool/9tv7fQ36nA?sr=wccr) [![Buy me a coffee with PayPal](https://img.shields.io/badge/Buy%20me%20a%20coffee-PayPal-0070BA?style=for-the-badge&logo=paypal&logoColor=white)](https://www.paypal.com/pool/9tv7fQ36nA?sr=wccr)

Correzioni al **servizio radio originale** dell'MG4 con infotainment **SAIC 1.0** (EH32 EU).

> ⚠️ **Solo per SAIC 1.0.** Non è per le versioni **Color Radio** né per il firmware **EH32 LL**
> (per quello c'è [MG4-Radio-DABplus-Fix](https://github.com/Skittle6938/MG4-Radio-DABplus-Fix)).
> Costruita per infotainment **@FW_SWI@**, servizio radio `@FW_VERSION@`.

> 🛑 **Questo APK è SOLO per @FW_SWI@** ed è stato **provato su MG4 Luxury**.
> Se la tua versione è diversa (R59, R63, R67, R71…) **non installarlo**: vedi *Hai un'altra versione del firmware?* più sotto.
> Se l'hai già installato per errore, lo togli anche senza computer con **RadioFixRemover** (vedi *Tornare all'originale*).

## Cosa corregge

- 📻 **FM bloccato su una stazione** — la lista stazioni non si riduce più a un'unica frequenza (es. resta sulla 90.90 e avanti/indietro non cambiano). Ora si aggiorna solo durante una vera ricerca.
- 🛡️ **Stabilità** — *novità v1.2.* Un promemoria DAB salvato senza ora non fa più bloccare il servizio a ogni avvio (sui firmware più vecchi riavviava tutto l'infotainment). Un errore in un'operazione in background non chiude più il servizio radio. Niente errore all'avvio se il sintonizzatore non è ancora pronto.
- 📶 **Cambio frequenza automatico (AF)** — Quando il segnale cala e il sintonizzatore passa da solo a un'altra frequenza della stessa stazione, la radio ora la segue invece di restare sulla frequenza vecchia. Serve **AF attivo** nelle impostazioni della radio.
- ▶️ **Ripresa all'accensione** — se stavi ascoltando la radio, quando riaccendi l'auto riparte da sola. Se l'avevi messa in pausa tu, o usavi un'altra sorgente (CarPlay, Bluetooth…), resta ferma.
- 🖼️ **Loghi DAB** — corretto un errore del firmware che non abbinava mai il logo all'ultima stazione ricevuta. Ora vengono accettati anche i loghi rettangolari, resi quadrati senza deformarli.
- 🏠 **Copertine DAB+ sulla schermata home** — le immagini trasmesse dalla stazione compaiono anche nella home, non solo nell'app Radio.
- ⚡ **Copertina immediata** — l'ultima immagine di ogni stazione viene salvata e mostrata subito quando ci torni.
- 🧹 **Memoria sotto controllo** — immagini e liste dei loghi non si accumulano più all'infinito.

La **grafica non cambia**: l'app Radio resta quella di fabbrica.
**Non migliora la ricezione**: il segnale debole o il DAB instabile dipendono dal sintonizzatore.

## Installazione da chiavetta USB (senza PC)

1. Scarica **`@APK@`** qui sotto, in **Assets**.
2. Copialo su una chiavetta USB **FAT32** o **exFAT**.
3. Inserisci la chiavetta nell'auto, apri il **gestore file**, tocca l'APK e conferma **Installa**: viene proposto come aggiornamento di un'app esistente.
4. **Riavvia l'unità** tenendo premuto **Home per circa 20 secondi**.
5. Apri la radio e **rifai una ricerca FM**.

> ⚠️ **Il riavvio è obbligatorio.** Senza riavvio continua a girare il servizio vecchio e sembra che non sia cambiato nulla.

**Non serve firmare niente né preparare altri file.** L'APK è già firmato con la stessa chiave di sistema dell'originale: per questo l'auto lo installa come aggiornamento dell'app di sistema, senza root e senza toccare la partizione di sistema.

**Con ADB:** `adb install -r @APK@` e poi `adb reboot`.

## Tornare all'originale

- **Senza PC, dalle Impostazioni:** Impostazioni → elenco app → servizio radio (`com.saicmotor.service.radio`) → **Disinstalla aggiornamenti**, poi riavvia. Questa voce non c'è su tutti i firmware.
- **Senza PC e senza ADB, con RadioFixRemover:** scarica **`RadioFixRemover_1.0.apk`** qui sotto in **Assets**, installalo da chiavetta come la patch, aprilo e tocca **Remove Radio Fix**. Poi **Restart head unit** e, dopo il riavvio, **Uninstall this helper app**.
- **Con ADB:** `adb uninstall com.saicmotor.service.radio` e poi `adb reboot`.

La versione di fabbrica resta intatta e torna automaticamente.

## Hai un'altra versione del firmware?

Questo APK è costruito per il servizio radio `@FW_VERSION@`. Con un'altra release SWI68 usa lo script `patcher/build.sh` del repository sul tuo APK originale: controlla da solo se il codice è compatibile e si ferma senza creare nulla se non lo è. Se si ferma, apri una **issue** indicando la tua versione SWI68.

## Verifica del file

SHA-256: `@SHA256@`

---
APK costruito e firmato automaticamente da GitHub Actions a partire dal codice di questo repository.
Progetto non ufficiale, non affiliato a SAIC Motor o MG. Modifichi il software della tua auto a tuo rischio.
Grazie a [Skittle6938](https://github.com/Skittle6938/MG4-Radio-DABplus-Fix) per l'idea delle copertine sulla home e della ripresa automatica.
