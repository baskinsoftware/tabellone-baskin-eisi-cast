# Compilazione, firma e pubblicazione — Tabellone Baskin EISI (Cast)

Guida a come compilare l'app in locale e a come i workflow GitHub producono gli
APK (stabile firmato e beta di debug).

---

## 1. Compilazione in locale

Requisiti: JDK 17 e Android SDK (`compileSdk 34`). Dalla cartella del progetto:

```bash
# APK di debug (firmato con la chiave di debug di Android) — versione "-beta"
./gradlew assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk   (versionName es. 26.1.1-beta)

# APK release (firmato solo se imposti le variabili keystore, vedi §3)
./gradlew assembleRelease
# output: app/build/outputs/apk/release/app-release.apk
```

Le build **debug** portano automaticamente il suffisso **`-beta`** nel
`versionName` (`versionNameSuffix "-beta"` in `app/build.gradle`), così sono
riconoscibili a colpo d'occhio rispetto alla release.

---

## 2. I workflow GitHub

Nella cartella `.github/workflows/`:

| Workflow | File | Quando parte | Cosa produce |
|---|---|---|---|
| **Release APK** | `release.yml` | push di un tag `v*` | APK **stabile firmato**, allegato alla Release |
| **Beta APK (unsigned)** | `beta.yml` | "Run workflow" manuale, o push sul branch `beta` | APK release **non firmato** come *artifact* del run (niente segreti) |
| **Prepara AAB per Google Play** | `play-prepare.yml` | push di un tag `v*`, o manuale | **AAB firmato** come *artifact*, per l'upload manuale su Play Console |
| **Crea release** | `crea-release.yml` | solo manuale (Actions → Crea release → Run workflow) | crea il **tag** e la **Release** (titolo e testo dal README) e avvia i workflow scelti coi due interruttori *APK firmato* e *AAB per Google Play* |

Dalla **26.1.1** `crea-release.yml` è lo stesso file della `release.yml` del
Tabellone Baskin EISI (PWA) — stessi parametri e stessa logica — adattato ai
workflow del Cast. Oltre a versione, ramo e prefisso del tag accetta:

| Parametro | A cosa serve |
|---|---|
| `readme` | percorso del README da cui prendere titolo e testo (default `README.md`) |
| `prerelease` | segna la Release come *pre-release* (per le beta `bN`) |
| `bozza` | crea la Release come **bozza**: non pubblicata, e non avvia i workflow |
| `apk` / `play_store` | interruttori dei due workflow, accesi per default |
| `altri_workflow` | altri file di workflow da avviare sul nuovo tag |

`release.yml` resta usabile da solo (push di un tag a mano): se la Release del
tag non esiste la crea lui, con la stessa regola del titolo; se esiste già, si
limita ad allegare l'APK senza toccarne titolo e testo. L'APK viene caricato
con `--clobber`, quindi ri-eseguire il workflow **sostituisce** il file invece
di accumulare copie.

Idea: `beta.yml` per i test rapidi (nessuna configurazione), `release.yml` quando
pubblichi una versione vera.

Il tag si può creare a mano (`git tag` + `git push origin <tag>`) oppure con
**Crea release**, che lo crea col token del workflow e poi avvia i workflow
scelti. Serve quando chi lavora sul repository può inviare rami ma non tag.
Nota: un tag creato dal token **non** fa partire i trigger `on: push: tags`
(regola di GitHub contro i cicli), ed è il motivo per cui `release.yml` ha anche
`workflow_dispatch` e viene avviato esplicitamente da `crea-release.yml`.

---

## 3. Configurazione della firma (una tantum)

Serve solo per gli APK **release** firmati. Imposta 4 *Secrets* nel repository
(**Settings → Secrets and variables → Actions → New repository secret**):

| Secret | Contenuto |
|---|---|
| `KEYSTORE_BASE64` | il tuo keystore `.jks` codificato in base64 |
| `KEYSTORE_PASSWORD` | password dello *store* |
| `KEY_ALIAS` | alias della chiave |
| `KEY_PASSWORD` | password della chiave |

Per generare `KEYSTORE_BASE64` dal tuo keystore:

```bash
# Linux
base64 -w0 mio.jks > keystore.b64
# macOS
base64 mio.jks > keystore.b64
```

Poi incolli il contenuto di `keystore.b64` nel secret.

Se non hai ancora un keystore, creane uno (conservalo con cura: è **per sempre**
per quel `applicationId`):

```bash
keytool -genkeypair -v -keystore mio.jks \
  -alias baskincast -keyalg RSA -keysize 2048 -validity 10000
```

**La chiave non entra mai nel repository:** in CI viene ricostruita in un file
temporaneo del runner (`$RUNNER_TEMP/release.jks`), usata per firmare e poi
cancellata da uno step `if: always()`.

### Nomi delle variabili di firma (dalla 26.1.1)

I *Secrets* qui sopra non cambiano mai nome. Cambia il nome con cui i workflow
li passano a Gradle, ora **identico a quello dell'app Android della PWA**:

| Variabile d'ambiente | Valore |
|---|---|
| `TBE_KEYSTORE_PATH` | percorso del `.jks` ricostruito (prima: `KEYSTORE_FILE`) |
| `TBE_KEYSTORE_PASSWORD` | dal secret `KEYSTORE_PASSWORD` |
| `TBE_KEY_ALIAS` | dal secret `KEY_ALIAS` |
| `TBE_KEY_PASSWORD` | dal secret `KEY_PASSWORD` |

In più, il `signingConfig` di `app/build.gradle` ora **non viene dichiarato**
se `TBE_KEYSTORE_PATH` non è impostata. Prima esisteva sempre, vuoto, e Gradle
provava comunque a usarlo: era la causa dell'errore
`KeytoolException: Tag number over 30 is not supported`, che si presentava
quando il file del keystore risultava assente o di 0 byte (secret non
impostato). Ora in quel caso la build release esce semplicemente **non
firmata**, che è anche il comportamento voluto in `beta.yml` e in locale.

Per firmare in locale:

```bash
export TBE_KEYSTORE_PATH="$HOME/chiavi/tabellone-cast.jks"
export TBE_KEYSTORE_PASSWORD='…'
export TBE_KEY_ALIAS='baskincast'
export TBE_KEY_PASSWORD='…'
./gradlew assembleRelease     # app/build/outputs/apk/release/app-release.apk
```

> ⚠️ **Se cambia la chiave, cambia la firma.** Android rifiuta di aggiornare
> un'app installata se l'APK nuovo è firmato con una chiave diversa
> (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`): chi ha la versione precedente deve
> **disinstallare e reinstallare**. Per lo stesso motivo, una volta pubblicata
> l'app su Google Play la chiave di *upload* non si può sostituire a piacere:
> serve passare dalla procedura di reset di Play Console. Conserva il keystore
> e le sue password fuori dal repository e fuori dalla macchina di sviluppo.

---

## 4. Pubblicare una release su GitHub

Modo consigliato: **Actions → Crea release → Run workflow**, con la versione
(es. `26.1.1`) e il ramo. Il workflow crea il tag, crea la Release col titolo e
il testo presi dal README e avvia i due workflow dell'app.

A mano, in alternativa:

```bash
# porta la versione avanti in app/build.gradle (versionCode + versionName)
git commit -am "Release 26.1.1"
git tag -a v26.1.1 -m "Tabellone Baskin EISI (Cast) 26.1.1"
git push origin main --tags
```

Il push del tag `v26.1.1` avvia `release.yml`, che crea la Release se non c'è e
allega `TabelloneBaskinCast-v26.1.1-signed.apk` (stabile firmato), e
`play-prepare.yml`, che prepara l'AAB per Google Play come artifact del run.

> Sulla numerazione delle versioni (`26.0`, `26.0.1`, `26.0.1.1`, …) vedi il riquadro
> "Versioni parallele Cast ⇄ PWA" nel [`README.md`](README.md).

---

## 5. Ottenere un APK beta al volo

Due modi, entrambi senza segreti:

- **Manuale**: tab **Actions** → **Beta APK (debug)** → **Run workflow**. A fine
  run scarichi l'APK dagli *Artifacts* del run.
- **Automatico**: fai push sul branch `beta` (`git push origin HEAD:beta`); il
  workflow parte da solo.

---

## 6. Verso il Play Store

- `applicationId` **immutabile** una volta pubblicato: `io.github.baskinsoftware.tabellonebaskineisicast`
  (diverso da quello dell'app della PWA, `io.github.baskinsoftware.tabellonebaskineisi`:
  sono due app distinte sullo store, anche se firmate con la stessa chiave).
- Per Play si consiglia l'**App Bundle** (`.aab`): `./gradlew bundleRelease`
  (output in `app/build/outputs/bundle/release/`). Si può aggiungere un workflow
  dedicato quando arrivi a quello step.
- La firma resta tua; valuta il **Play App Signing** (Google custodisce la chiave
  di distribuzione, tu firmi con la upload key).
- Per la sezione **Android TV** servono un **banner 320×180** e screenshot TV.

---

## 7. Se il primo run CI fallisce

Non potendo eseguire la CI in fase di sviluppo, verifica al primo giro:

- che `compileSdk 34` sia disponibile sul runner (l'SDK è già installato sulle
  immagini `ubuntu-24.04`: lo step `android-actions/setup-android` è stato
  tolto dopo che un suo aggiornamento aveva rotto tutte le build);
- che il `gradlew` sia eseguibile (i workflow fanno già `chmod +x ./gradlew`);
- in caso di errore di firma, che i 4 secret siano impostati e che
  `KEYSTORE_BASE64` sia il base64 **esatto** del `.jks` (senza a capo/spazi).

Con il log dell'errore la messa a punto è rapida.

---

*Autore: Daniele Lolli (UncleDan) — Tabellone Baskin EISI (Cast) 26.1.1.*
