# Tabellone Baskin EISI (Cast)

App Android che incorpora il tabellone segnapunti per il Baskin e lo trasmette
su un televisore in rete locale.

> ⚠️ **Work in progress.** Questa versione Android è in **sviluppo
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
[`LICENSE`](LICENSE)). Tutte le dipendenze arrivano da Maven Central e Google
Maven, che sono repository **ammessi** dalla policy F‑Droid: vedi
[`F-DROID.md`](F-DROID.md) per lo stato della candidatura e cosa manca.

**Autore:** Daniele Lolli (UncleDan) · **Licenza:** MIT · **Versione:** 26.1.2
(incorpora la PWA Tabellone Baskin EISI 26.1.2)

> ℹ️ **Versioning Baskin v3 (dalla 26.1.2):** il Cast segue la regola unica di
> tutti i progetti Baskin — **`AA.M[.P][.W][bN]`**: `AA` anno a due cifre, `M`
> progressivo di release dell'anno (*non* il mese), `P` patch, `W` revisione del
> **solo wrapper**, `bN` beta. Il numero **rispecchia la PWA incorporata**
> (PWA 26.1.2 → Cast 26.1.2); un fix del solo wrapper (nativo, icone, CI,
> firma, dipendenze) aggiunge `W` senza toccare le cifre della PWA
> (`26.1.2 → 26.1.2.1`), e `W` torna a 0 al re-sync successivo. Quando la PWA
> ha solo due cifre la `P` si scrive a zero per fare posto a `W`: `26.1` + W 1
> dà **`26.1.0.1`**, perché `26.1.1` significa già la patch 1 della PWA.
>
> `W` esiste solo nei **wrapper separati** come questo: nei repository dove app
> e PWA condividono un unico numero (Tabellone PWA, Classificazione) non si usa.
>
> Il `versionCode` è a **9 cifre**, `AA MM PP W BB`, dove `BB` è il contatore
> unico delle pre-release (`01`–`98`) e `99` indica la finale:
>
> | Versione | `versionCode` |
> |---|---|
> | `26.1.2` | `260102099` |
> | `26.1.2b1` | `260102001` |
> | `26.1.2.1` | `260102199` |
> | `26.2` | `260200099` |
>
> Ogni beta sta così sotto la propria finale e il numero cresce sempre, come
> richiede Google Play.
>
> In `app/build.gradle` la fonte di verità è `pwaVersion`, `targetPwa`,
> `wrapper` e `beta`; i valori restano scritti come letterali (serve a F‑Droid,
> che li legge staticamente), ma un **controllo automatico blocca la build** se
> non rispettano la regola. La versione della PWA è anche leggibile a runtime
> via `BuildConfig.PWA_VERSION`.
>
> <sub>Fino alla 26.1.1 il Cast usava uno schema proprio, con `versionCode` a
> 6 cifre (`26.1 → 260100`) e la revisione del wrapper in terza posizione: due
> situazioni diverse potevano produrre lo stesso `versionName`. Prima della
> 26.0 si usava una "doppia versione" (`1.3.3 (PWA 2026d)`), abbandonata perché
> poco conforme agli store.</sub>

---

---

## Progetti collegati

Il Tabellone Baskin esiste in **due repository**, che lavorano insieme:

- **PWA (web)** — repo **[`tabellone-baskin-eisi-pwa`](https://github.com/baskinsoftware/tabellone-baskin-eisi-pwa)**:
  il segnapunti web, installabile e offline, ospitabile su GitHub Pages
  (online su <https://baskinsoftware.github.io/tabellone-baskin-eisi-pwa/>). È anche il *display*
  via browser di questa app.
- **App Android (Cast)** — repo **[`tabellone-baskin-eisi-cast`](https://github.com/baskinsoftware/tabellone-baskin-eisi-cast)** *(questo, work in progress)*:
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
app/src/main/java/io.github.baskinsoftware.tabellonebaskineisicast/
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

> ℹ️ **"Chiudi applicazione" (dalla 1.3.2):** la voce di menu della PWA chiama
> `window.close()`. Dentro una WebView Android questo veniva ignorato
> silenziosamente senza `WebChromeClient.onCloseWindow()` (bug corretto in
> `MainActivity.java`, che ora chiude l'intero task con `finishAffinity()`).
> `CastBridge` espone anche un metodo dedicato `quitApp()`, pronto per
> un'integrazione esplicita lato PWA sorgente (stesso pattern usato per
> `openRoleSettings`/`showConnectInfo`), se in futuro si preferisce non
> dipendere dal comportamento di `window.close()`.

## Localizzazione

Dalla **1.2.0**, tutte le stringhe utente del framework nativo (schermata
scelta ruolo, dialog di connessione, toast di stato) sono estratte in risorse
Android e tradotte in **italiano (base), inglese e francese**:

```
app/src/main/res/values/strings.xml      italiano (default/fallback)
app/src/main/res/values-en/strings.xml   inglese
app/src/main/res/values-fr/strings.xml   francese
```

Android sceglie automaticamente la lingua in base a quella di sistema del
dispositivo, con fallback all'italiano se non è tra quelle disponibili.
Questo è indipendente dalla lingua della PWA incorporata (che dalla 1.18.0 ha
la propria selezione IT/EN/FR gestita da `js/i18n.js`): le due localizzazioni
non sono collegate, cambiare una non cambia l'altra.

Se aggiungi nuove stringhe nel codice nativo, ricordati di estrarle in tutti
e tre i file `strings.xml` (non lasciarle mai hardcoded in Java), altrimenti
compare in una sola lingua indipendentemente dal dispositivo.

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

La Release si crea dal workflow **`crea-release.yml`** (*Actions → Crea
release*), che compone titolo e testo dal README, crea il tag e avvia i
workflow dell'APK e dell'AAB. È lo stesso file — stessi parametri, stessa
logica — della `release.yml` della PWA.

> ℹ️ **Firma dell'APK (dalla 26.1.1):** i segreti restano quelli sopra, ma i
> workflow li passano a Gradle con i nomi **`TBE_KEYSTORE_PATH`**,
> `TBE_KEYSTORE_PASSWORD`, `TBE_KEY_ALIAS`, `TBE_KEY_PASSWORD` — gli stessi
> dell'app Android della PWA, così i due progetti si firmano allo stesso modo.
> Il `signingConfig` ora non viene nemmeno **dichiarato** se il keystore manca
> (prima esisteva sempre, vuoto: era la causa dell'errore `Tag number over 30
> is not supported` quando il file del keystore era assente). Dettagli in
> [`COMPILAZIONE-E-RELEASE.md`](COMPILAZIONE-E-RELEASE.md).

## Verso il Play Store

- `applicationId` **immutabile**: `io.github.baskinsoftware.tabellonebaskineisicast`.
- Cleartext HTTP consentito ma l'app è **solo LAN** (nessun traffico internet).
- Per la sezione Android TV servirà un **banner** e gli screenshot dedicati.

---

**Autore:** Daniele Lolli (UncleDan) — **Versione:** 26.1.2
