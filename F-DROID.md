# Pubblicazione su F‑Droid

Stato e passi per candidare **Tabellone Baskin EISI (Cast)** al repository
principale di F‑Droid.

---

## Premessa: il vincolo che credevamo di avere non esiste

Nelle versioni precedenti il README diceva che l'app *«non applica i vincoli
F‑Droid perché usa NanoHTTPD/ZXing da Maven Central»*. **Non è corretto**, ed è
stato corretto.

La [Inclusion Policy](https://f-droid.org/en/docs/Inclusion_Policy/) di F‑Droid
elenca esplicitamente i repository Maven considerati affidabili, tra cui
**Maven Central** e **Google Maven**. Usare dipendenze da lì non è un ostacolo:
l'unico requisito è che le librerie siano **FLOSS**. Le nostre lo sono tutte:

| Dipendenza | Licenza | Origine |
|---|---|---|
| `androidx.webkit` | Apache‑2.0 | Google Maven |
| `org.nanohttpd:nanohttpd` | BSD‑3‑Clause | Maven Central |
| `com.google.zxing:core` | Apache‑2.0 | Maven Central |

Non c'è traccia di Google Play Services, Firebase, GMS, pubblicità o tracker:
proprio le cose che F‑Droid rifiuta.

---

## Cosa è già a posto

- [x] **Licenza FLOSS**: MIT, con file [`LICENSE`](LICENSE) nel repo
- [x] **Codice sorgente pubblico**: il repository è pubblico e contiene il codice reale
- [x] **Dipendenze tutte FLOSS**, da repository Maven ammessi (tabella sopra)
- [x] **Nessun blob binario** nel sorgente (solo `gradle/wrapper/gradle-wrapper.jar`, standard e gestito da F‑Droid)
- [x] **Toolchain FLOSS**: Gradle + Android Gradle Plugin, compilabile da riga di comando senza IDE proprietari
- [x] **Application ID su dominio proprio**: `it.uncledan.baskintabellonecast`, coerente con la convenzione richiesta da F‑Droid
- [x] **Tag git per ogni release** (`vX.Y.Z-pwa<versione>`), necessari per `UpdateCheckMode: Tags`
- [x] **Firma compatibile**: il `signingConfig` si attiva solo se esistono le variabili d'ambiente del keystore. Nell'ambiente di build di F‑Droid non ci sono, quindi l'app viene compilata non firmata e **firmata da F‑Droid** con la propria chiave, come richiesto
- [x] **Nessuna telemetria**: l'app opera solo in rete locale, non contatta server remoti
- [x] **Metadati Fastlane** in `fastlane/metadata/android/` (it‑IT, en‑US, fr‑FR): titolo, descrizioni e changelog
- [x] **Bozza del file di metadati** per fdroiddata in [`fdroid/it.uncledan.baskintabellonecast.yml`](fdroid/it.uncledan.baskintabellonecast.yml)

---

## Cosa manca ancora

### 1. Loghi di terzi — il punto da chiarire prima di candidarsi ⚠️

Gli asset `app/src/main/assets/pwa/icons/logos/logo-baskin.svg` e
`logo-eisi.svg` sono i loghi ufficiali del **Baskin** e di **EISI**, e non sono
opera dell'autore dell'app. Il nome interno del file dichiara `Logo Baskin (R)`,
cioè un **marchio registrato**.

Il file `LICENSE` oggi copre codice, PWA e suoni, ma **non dice nulla su questi
due loghi**. F‑Droid, in fase di revisione, controlla proprio questo genere di
cose e raccomanda esplicitamente di stare alla larga da violazioni di marchio e
copyright.

La policy ammette che gli asset "non funzionali" (grafica) stiano sotto licenze
meno permissive del codice, ma la situazione va **dichiarata**, non lasciata
implicita. Le strade possibili:

1. **Autorizzazione scritta** da EISI / dai titolari del marchio Baskin all'uso
   dei loghi in questa app, citata nel `LICENSE` (es. *«loghi Baskin ed EISI usati
   su concessione di …, tutti i diritti riservati ai rispettivi titolari»*).
   È la soluzione migliore e più pulita.
2. **Rimuovere i loghi** dalla PWA incorporata e sostituirli con grafica
   originale, se ottenere l'autorizzazione risultasse complicato.

Finché questo punto non è risolto, meglio non aprire la candidatura: un rilievo
sul marchio in fase di revisione è una delle cause tipiche di rifiuto.

> Nota pratica: i loghi arrivano dalla PWA a monte, quindi la scelta va fatta
> lì (nel repo `tabellone-baskin-eisi-pwa`), altrimenti ogni re‑sync li
> reintroduce.

### 2. Screenshot

Le cartelle sono pronte ma vuote:

```
fastlane/metadata/android/<locale>/images/phoneScreenshots/     telefono/tablet
fastlane/metadata/android/<locale>/images/tenInchScreenshots/   tablet 10"
fastlane/metadata/android/<locale>/images/tvScreenshots/        Android TV
```

Servono PNG o JPG reali (bastano 2–4 per formato: schermata ruolo, tabellone in
uso, vista su TV). Vanno messi almeno per `it-IT`; le altre lingue possono
riusare gli stessi file.

### 3. Metadati dei loghi da ripulire (minore)

I due SVG contengono metadati Inkscape con percorsi locali del PC dell'autore
(`G:\Il mio Drive\Documents\...`). Non è un ostacolo per F‑Droid, ma è
informazione personale inutile pubblicata nel repo: conviene ripulirli, sempre
a monte nella PWA.

### 4. Nota sul versionName — risolta dalla 26.0

Il vecchio `versionName` composto (`1.3.3 (PWA 2026d)`) era costruito per
interpolazione da due variabili gradle e `fdroidserver` non riusciva a
ricavarlo: l'aggiornamento automatico dei metadati non avrebbe funzionato.

Dalla **26.0** il progetto usa lo schema `<versione>.<sottoversione>.<patch>`,
con la versione allineata all'anno della PWA incorporata (serie `26.x` per il
2026). Il `versionName` è ora una stringa semplice assegnata da una variabile
gradle, che `fdroidserver` sa leggere: `AutoUpdateMode: Version` è quindi
impostato nella bozza dei metadati. Conviene comunque **verificare i primi
aggiornamenti automatici** prima di considerarli affidabili.

---

## Procedura di candidatura, quando i punti sopra sono risolti

1. Verificare che l'ultima release sia taggata e che il tag compili da solo:
   ```bash
   git clone https://github.com/UncleDan/tabellone-baskin-eisi-cast.git
   cd tabellone-baskin-eisi-cast
   git checkout v26.0
   ./gradlew assembleRelease
   ```
   Deve concludersi senza chiedere keystore né credenziali.
2. Aprire una **merge request** su [fdroiddata](https://gitlab.com/fdroid/fdroiddata)
   aggiungendo `metadata/it.uncledan.baskintabellonecast.yml`, partendo dalla
   bozza in [`fdroid/`](fdroid/).
3. In alternativa, aprire prima una *Request For Packaging* nel
   [tracker di fdroiddata](https://gitlab.com/fdroid/fdroiddata/-/issues) e
   lasciare che sia un contributore a preparare la MR.
4. Rispondere ai rilievi dei revisori. Se chiedono chiarimenti sulle licenze
   dei loghi, serve l'autorizzazione del punto 1.
5. Ad ogni release successiva: nuovo tag + aggiornamento manuale del file di
   metadati (nuova voce `Builds:` + `CurrentVersion`/`CurrentVersionCode`) +
   nuovo changelog in `fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt`.

---

## Alternativa: repository F‑Droid proprio

Se la questione dei loghi non fosse risolvibile, resta possibile distribuire
l'app tramite un **repository F‑Droid personale** (con `fdroidserver`, ospitabile
anche su GitHub Pages). Gli utenti lo aggiungono a mano nel client F‑Droid. In
quel caso le regole del repo principale non si applicano, ma l'app non compare
nel catalogo ufficiale e non gode della stessa visibilità né della firma di
F‑Droid.
