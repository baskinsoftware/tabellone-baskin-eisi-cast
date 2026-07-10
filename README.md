# Tabellone Baskin Cast — app Android

> ⚠️ **Work in progress.** Questa versione Android (Cast) è in **sviluppo
> attivo**: funzioni e comportamenti possono cambiare, e alcune parti non sono
> ancora state provate su tutti i dispositivi. Per l'uso stabile e immediato c'è
> la **PWA** (vedi *Progetti collegati*).

## Cos'è il Baskin

Il Baskin è uno sport inclusivo per progettazione, nato in Italia nei primi anni duemila, che prevede per regolamento la partecipazione di atleti di entrambi i sessi con e senza disabilità. Il regolamento del Baskin è una proprietà intellettuale di *Associazione Baskin* ed i campionati in Italia sono organizzati da *EISI - Ente Italiano Sport Inclusivi*, Ente di Promozione Paralimpica riconosciuto dal *Comitato Italiano Paralimpico*.

Per approfondire: [baskin.it](https://baskin.it) · [eisi.it](https://eisi.it)

---

## L'app

App **unica e adattiva** per il **Baskin**: gestisce la partita (tempo, punti,
falli, timeout) e **presenta il tabellone su un TV** in sola visualizzazione,
senza fili, in modo semplice.

> 📖 **[Manuale operativo completo → `MANUALE.md`](MANUALE.md)** — tutti i modi
> d'uso, passo passo (compreso perché il *controller* deve essere Android).

È **FOSS**: codice rilasciato sotto licenza **MIT** (vedi [`LICENSE`](LICENSE)).
Compilabile con Gradle + AndroidX + NanoHTTPD + ZXing (librerie di terzi anch'esse
FOSS: NanoHTTPD BSD‑3‑Clause, ZXing e AndroidX Apache‑2.0 — attribuzioni in
[`LICENSE`](LICENSE)). Non applica i vincoli F‑Droid (usa NanoHTTPD/ZXing da Maven
Central), ma resta interamente open source.

**Autore:** Daniele Lolli (UncleDan) · **Licenza:** MIT · **Versione:** 1.1.4 (PWA 1.17.4)
(framework 1.1.4 · incorpora la PWA Tabellone Baskin 1.17.4)

> ℹ️ **Doppia versione (attiva dalla 1.1.0):** il `versionName` mostra sia la
> versione del **framework** nativo sia quella della **PWA** incorporata (es.
> `1.1.4 (PWA 1.17.4)`), leggibili dall'"info app" di Android. In `build.gradle`
> sono definite separatamente (`frameworkVersion`, `pwaVersion`) ed esposte anche
> come `BuildConfig.FRAMEWORK_VERSION` / `BuildConfig.PWA_VERSION`. Ad ogni
> re-sync degli asset si aggiorna `pwaVersion` e si incrementa il `versionCode`.

---

---

## Progetti collegati

Il Tabellone Baskin esiste in **due repository**, che lavorano insieme:

- **PWA (web)** — repo **[`baskin-tabellone`](https://github.com/UncleDan/baskin-tabellone)**:
  il segnapunti web, installabile e offline, ospitabile su GitHub Pages
  (online su <https://www.uncledan.it/tabellone-baskin/>). È anche il *display*
  via browser di questa app.
- **App Android (Cast)** — repo **[`baskin-tabellone-cast`](https://github.com/UncleDan/baskin-tabellone-cast)** *(questo, work in progress)*:
  incorpora la stessa PWA e aggiunge i ruoli, la scoperta di rete e la
  presentazione su TV.

Come si combinano le due versioni è spiegato nel
**[Manuale operativo](MANUALE.md)**.

---

## Ruoli (app unica, adattiva)

Lo stesso APK può fare da:

- **Tabellone** (*controller / master*): comandi + fonte di verità. **Deve essere
  un dispositivo Android** con questa app: fa da server sulla rete e si annuncia
  da solo (un browser non può farlo).
- **Schermo** (*display*): mostra il tabellone in sola visualizzazione su un TV.

**Scelta del ruolo:** proposta al **primo avvio** con preselezione automatica
(TV → *Schermo*, telefono → *Tabellone*), modificabile poi da `…` → **Ruolo
dispositivo…** (sul controller) o **tenendo premuto** sullo schermo.

## Come arriva il tabellone sul TV

1. **App su chiavetta / Android TV** (consigliato): lo schermo **trova da solo**
   il tabellone via **NSD** (mDNS) e si aggancia. *Accendi e vai.*
2. **Browser della smart TV**: sul controller `…` → **Indirizzo / QR per la TV**;
   sul TV apri `http://IP:porta` (o inquadra il **QR**). Nessuna app sul TV.
3. **Display secondario** del controller stesso (USB‑C→HDMI / Miracast): il
   tabellone appare sullo schermo esterno, i comandi restano sul telefono.

## Architettura (perché non c'è grafica duplicata)

Il rendering del tabellone è **sempre la stessa PWA** in modalità
`?display=1` (una sola estetica). Cambia solo *chi tiene il server*:

```
[ Controller Android ]  --CastBridge.pushState-->  [ CastServer (NanoHTTPD) ]
   (PWA con comandi)                                   | annuncio NSD (_baskin._tcp)
                                                       | serve /  e  /state
                          polling GET /state           |
   [ Schermo: app su chiavetta/TV ]  <----------------- |   (trovato via NSD)
   [ Schermo: browser del TV ]       <----------------- |   (indirizzo/QR)
   [ Display secondario HDMI/Miracast ] <-------------- |   (127.0.0.1)
```

## Componenti nativi

```
app/src/main/java/it/uncledan/baskintabellonecast/
├── RoleActivity.java       primo avvio: scelta ruolo + instradamento (LAUNCHER)
├── Roles.java              modello ruoli + persistenza + rilevamento TV/telefono
├── MainActivity.java       ruolo Tabellone: WebView comandi + server + NSD + QR
├── DisplayActivity.java    ruolo Schermo: scoperta NSD + WebView ?display=1
├── CastServer.java         web server NanoHTTPD (PWA + /state)
├── CastBridge.java         ponte JS→nativo (stato, ruolo, info connessione)
├── ScorePresentation.java  display secondario (Presentation API)
└── QrUtil.java             QR (ZXing) per il collegamento via browser
app/src/main/assets/pwa/    la PWA Tabellone Baskin (controllo + modalità display)
```

## Permessi

- **INTERNET**: web server integrato, scoperta NSD, display secondario. Nessun
  server remoto: solo rete locale.
- `res/xml/network_security_config.xml`: consente HTTP in chiaro (necessario in
  LAN). Android non permette di limitarlo a un intervallo di IP; l'app comunque
  non contatta internet.

## Compatibilità TV

Il manifest dichiara `touchscreen` e `leanback` **non richiesti** e registra il
**LEANBACK_LAUNCHER**, così l'app si installa e compare anche su **Android TV**.
*(Per la pubblicazione su Play in sezione TV servirà un banner 320×180.)*

## Build in locale

```bash
./gradlew assembleDebug     # APK di debug non firmato
./gradlew assembleRelease   # APK release (firmato se sono presenti le variabili keystore)
```

## Pubblicazione dell'APK su GitHub (automatica)

È incluso il workflow **`.github/workflows/release.yml`**: al push di un tag
`v*` compila, **firma** e allega l'APK alla Release. Richiede questi *Secrets*
nel repository: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
`KEY_PASSWORD`. La chiave non entra mai nel repo.

## Verso il Play Store

- `applicationId` **immutabile**: `it.uncledan.baskintabellonecast`.
- Cleartext HTTP consentito ma l'app è **solo LAN** (nessun traffico internet).
- Per la sezione Android TV servirà un **banner** e gli screenshot dedicati.

---

**Autore:** Daniele Lolli (UncleDan) — **Versione:** 1.1.4 (PWA 1.17.4)
