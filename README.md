# Tabellone Baskin Cast — app Android

Stesse funzionalità del **Tabellone Baskin**, con in più la possibilità di
**presentare il tabellone su un TV in sola visualizzazione** (senza comandi):

1. **Display secondario** (HDMI, Miracast, "Trasmetti schermo"): il tabellone
   viene mostrato sullo schermo esterno tramite la *Presentation API* di
   Android, mentre il telefono/tablet tiene i comandi.
2. **LAN** (browser del TV): l'app avvia un piccolo **web server** integrato;
   dal browser della smart TV apri `http://IP-telefono:porta` e vedi il
   tabellone a schermo intero.

È **FOSS** e compilabile con solo Gradle + AndroidX + NanoHTTPD. Non è pensata
per F-Droid (usa NanoHTTPD da Maven Central e non applica i vincoli F-Droid),
ma resta interamente open source.

**Autore:** Daniele Lolli (UncleDan) · **Licenza:** MIT · **Versione:** 1.0.0
(incorpora la PWA Tabellone Baskin 1.15.0)

---

## Come funziona

- Il **WebView di controllo** carica la PWA completa (con i comandi) dagli
  `assets/` tramite `WebViewAssetLoader` (origine sicura `https://appassets…`).
- Ad ogni cambio di stato la PWA chiama `window.CastBridge.pushState(json)`: un
  ponte JavaScript→nativo (`CastBridge`) che aggiorna il **web server
  integrato** (`CastServer`, basato su NanoHTTPD).
- Il **display** (secondario o TV) apre la stessa PWA in **modalità sola
  visualizzazione** (`index.html?display=1`): nasconde tutti i comandi e
  interroga `GET /state` ogni ~0,75 s. Il cronometro scorre fluido perché il
  tempo avanza in locale tra un aggiornamento e l'altro (`running` +
  `remainingMs`).
- Il **display secondario** è un `WebView` dentro una `Presentation` che punta a
  `http://127.0.0.1:porta/?display=1`: stesso identico percorso di un TV sulla
  LAN, così i due trasporti condividono tutto il codice.

Schema:

```
[ WebView controllo ]  --CastBridge.pushState-->  [ CastServer (NanoHTTPD) ]
   (PWA con comandi)                                  |  serve /  e  /state
                                                      |
                        polling GET /state            |
   [ Presentation su display esterno ] <-------------- |
   [ Browser del TV su LAN ]           <-------------- |
        (index.html?display=1, sola visualizzazione)
```

## Uso

- **Display secondario**: collega il telefono al TV (adattatore USB‑C→HDMI, o
  "Trasmetti schermo"/Miracast). Appena il sistema espone un display di
  presentazione, l'app ci mostra automaticamente il tabellone; il telefono
  resta con i comandi.
- **LAN**: all'avvio l'app mostra un avviso con l'indirizzo
  `http://IP-telefono:porta` (di default porta 8080). Apri quell'indirizzo nel
  **browser del TV** (stessa rete Wi‑Fi): comparirà il tabellone senza comandi.

> Il telefono e il TV devono essere sulla **stessa rete locale**. Il traffico è
> in chiaro (HTTP) verso la LAN: normale per un dispositivo locale.

## Permessi

- **INTERNET**: necessario per il web server integrato (LAN) e per il display
  secondario. L'app **non contatta server remoti**: serve solo contenuti locali.
- Il file `res/xml/network_security_config.xml` consente il traffico HTTP in
  chiaro verso la rete locale (necessario perché TV e display leggono via HTTP).

## Build in locale

```bash
# APK di debug
./gradlew assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk
```

Per un APK release firmato, configura un tuo keystore (vedi la documentazione
Android su `signingConfigs`).

## Struttura (parte nativa)

```
app/src/main/java/it/uncledan/baskintabellonecast/
├── MainActivity.java       WebView controllo + server + display secondario + URL LAN
├── CastServer.java         web server NanoHTTPD (serve la PWA + /state)
├── CastBridge.java         ponte JS→nativo (window.CastBridge.pushState)
└── ScorePresentation.java  contenuto del display secondario (WebView ?display=1)
app/src/main/assets/pwa/    la PWA Tabellone Baskin (controllo + modalità display)
```

## Note e limiti

- La modalità **sola visualizzazione** della PWA è attivata dal parametro
  `?display=1`: nasconde play, modifica, menu, pulsanti punti/falli,
  sirena/fischietto; mostra solo punteggi, tempo, periodo, timeout, falli/bonus,
  possesso e nomi. In questa modalità la PWA **non modifica né salva** la partita
  e **non registra il service worker** (è un puro visualizzatore live).
- Restano disponibili tutte le funzioni del tabellone sul dispositivo di
  controllo, incluso lo **Streaming BaskinCam** opzionale.
- "Bluetooth verso il TV" non è previsto: i TV non accettano un flusso schermo
  via Bluetooth. I trasporti supportati sono display secondario e LAN.

---

**Autore:** Daniele Lolli (UncleDan)
**Versione:** 1.0.0
