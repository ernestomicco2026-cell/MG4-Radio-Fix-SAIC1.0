Correzioni al **servizio radio originale** dell'MG4 con infotainment **SAIC 1.0** (EH32 EU).

> ⚠️ **Solo per SAIC 1.0.** Non è per le versioni **Color Radio** né per il firmware **EH32 LL**
> (per quello c'è [MG4-Radio-DABplus-Fix](https://github.com/Skittle6938/MG4-Radio-DABplus-Fix)).
> Costruita per infotainment **@FW_SWI@**, servizio radio `@FW_VERSION@`.

## Cosa corregge

- 📻 **FM bloccato su una stazione** — la lista stazioni non si riduce più a un'unica frequenza (es. resta sulla 90.90 e avanti/indietro non cambiano). Ora si aggiorna solo durante una vera ricerca.
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

- **Senza PC:** Impostazioni → elenco app → servizio radio (`com.saicmotor.service.radio`) → **Disinstalla aggiornamenti**, poi riavvia. Questa voce non c'è su tutti i firmware.
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
