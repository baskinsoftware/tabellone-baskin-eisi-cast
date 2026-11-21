# Compilazione, firma e pubblicazione — Tabellone Baskin EISI (Cast)

Guida a come compilare l'app in locale e a come i workflow GitHub producono gli
APK (stabile firmato e beta di debug).

---

## 1. Compilazione in locale

Requisiti: JDK 17 e Android SDK (`compileSdk 34`). Dalla cartella del progetto:

```bash
# APK di debug (firmato con la chiave di debug di Android) — versione "-beta"
./gradlew assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk   (versionName es. 26.1b2-beta)

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
| **App Android** | `app-android.yml` | push su `beta` o `main`, push di un tag `v*`, o manuale | APK **debug** sempre, APK **release firmato** se ci sono i segreti; su `beta` aggiorna la pre-release `app-beta`, su un tag allega gli APK alla Release |
| **Release** | `release.yml` | solo manuale (Actions → Release → Run workflow) | crea la Release (titolo e testo dal README, tag dalla versione) e avvia `app-android.yml` sul nuovo tag |
| **Prepara AAB per Google Play** | `play-prepare.yml` | push di un tag `v*`, o manuale | **AAB firmato** come *artifact*, per l'upload manuale su Play Console |

Idea: si lavora su `beta` e si prende l'APK dalla pre-release `app-beta`; quando
la versione è buona si lancia **Release**, che fa tag, Release e APK definitivi.
Una versione con `b` (es. `26.1b1`) diventa pre-release automaticamente.

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
temporaneo del runner, usata per firmare e poi cancellata. In locale, senza le
variabili d'ambiente `KEYSTORE_FILE/…`, la build release **non** viene firmata
(così il debug locale resta invariato).

---

## 4. Pubblicare una release su GitHub

```bash
# porta la versione avanti in app/build.gradle (versionCode + versionName)
git commit -am "Release 26.1"
git tag -a v26.1 -m "Tabellone Baskin EISI (Cast) 26.1"
git push origin main --tags
```

Il push del tag `v26.1` avvia `app-android.yml`, che allega alla Release
`Tabellone-Baskin-EISI-Cast-26.1.apk` (stabile firmato), e `play-prepare.yml`,
che prepara l'AAB per Google Play come artifact del run.

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

- `applicationId` **immutabile** una volta pubblicato: `it.uncledan.tabellonebaskineisicast`.
- Per Play si consiglia l'**App Bundle** (`.aab`): `./gradlew bundleRelease`
  (output in `app/build/outputs/bundle/release/`). Si può aggiungere un workflow
  dedicato quando arrivi a quello step.
- La firma resta tua; valuta il **Play App Signing** (Google custodisce la chiave
  di distribuzione, tu firmi con la upload key).
- Per la sezione **Android TV** servono un **banner 320×180** e screenshot TV.

---

## 7. Se il primo run CI fallisce

Non potendo eseguire la CI in fase di sviluppo, verifica al primo giro:

- che `android-actions/setup-android@v3` accetti le licenze SDK (di norma sì) e
  che `compileSdk 34` sia disponibile sul runner;
- che il `gradlew` sia eseguibile (i workflow fanno già `chmod +x ./gradlew`);
- in caso di errore di firma, che i 4 secret siano impostati e che
  `KEYSTORE_BASE64` sia il base64 **esatto** del `.jks` (senza a capo/spazi).

Con il log dell'errore la messa a punto è rapida.

---

*Autore: Daniele Lolli (UncleDan) — Tabellone Baskin EISI (Cast).*
