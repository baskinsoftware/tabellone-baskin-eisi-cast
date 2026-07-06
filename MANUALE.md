# Manuale operativo — Tabellone Baskin Cast

Questo manuale spiega **tutti i modi** per usare l'app e mostrare il tabellone su
un TV. È pensato per essere semplice: nella maggior parte dei casi basta
accendere e aprire l'app.

> ⚠️ **L'app Android (Cast) è work in progress.** Se cerchi qualcosa di stabile e
> immediato, la **PWA** funziona già da sola in un browser.

---

## 0. Le due versioni e i due repository

Il Tabellone Baskin vive in **due repository GitHub** e in **due versioni** che
si integrano:

- **PWA (web)** — repo **[`baskin-tabellone`](https://github.com/UncleDan/baskin-tabellone)**.
  È il segnapunti web: si apre in un browser, si installa, funziona offline
  (online: <https://www.uncledan.it/tabellone-baskin/>). Da sola mostra il
  tabellone su **uno** schermo. In modalità `?display=1` fa da **schermo (display)
  via browser** per l'app.
- **App Android — "Cast"** — repo **[`baskin-tabellone-cast`](https://github.com/UncleDan/baskin-tabellone-cast)**
  *(work in progress)*. Incorpora **la stessa** PWA e aggiunge i ruoli, la
  scoperta di rete (NSD), il web server e la presentazione su TV.

**Come si usano insieme, in pratica:**

| Vuoi… | Usa |
|---|---|
| Solo il segnapunti su un dispositivo | **PWA** (repo `baskin-tabellone`), da browser |
| Comandare la partita | **App Cast** in ruolo *Tabellone* — **deve essere Android** |
| Mostrare il tabellone su un TV con un'app | **App Cast** in ruolo *Schermo* su una chiavetta/Android TV |
| Mostrare il tabellone su un TV con un browser | **PWA** aperta sul browser del TV all'indirizzo dato dall'app Cast |

In breve: **la PWA è la parte "schermo/segnapunti"; l'app Cast aggiunge la parte
"comando + trasmissione".** L'app incorpora già la PWA, quindi non devi
installare nulla di separato per il rendering.

---

## 1. Concetti di base

L'app è **una sola**, ma può assumere due **ruoli**:

- **Tabellone** (detto anche *controller* o *master*): è chi **gestisce la
  partita** — tempo, punti, falli, timeout. È la **fonte di verità** e
  distribuisce il tabellone agli schermi.
- **Schermo** (detto anche *display*): mostra il tabellone **in sola
  visualizzazione**, senza comandi, tipicamente su un TV.

Lo stesso APK fa entrambe le cose: sceglie il ruolo al primo avvio (con una
proposta già impostata) e lo puoi cambiare quando vuoi dalle impostazioni.

---

## 2. Importante: **il "Tabellone" (controller) deve essere Android**

Chi comanda la partita **deve essere un dispositivo Android con questa app**
(telefono o tablet). Non può essere una pagina web nel browser.

Perché? Il controller, oltre a mostrare i comandi, deve **mettersi in ascolto
sulla rete** per servire il tabellone agli schermi (fa da piccolo server e si
annuncia da solo). Una pagina web dentro un browser, per motivi di sicurezza,
**non può aprire un server né farsi trovare** sulla rete. Quindi:

- ✅ **Controller = app Android** → obbligatorio.
- ✅ **Schermo = app Android** *oppure* **un semplice browser** (vedi sotto).

In pratica: in ogni collegamento serve **almeno un'app Android**, ed è quella che
fa da controller.

---

## 3. Primo avvio e cambio ruolo

**Al primo avvio** l'app chiede il ruolo del dispositivo, con una **scelta già
preselezionata**:

- se apri l'app su un **TV / box Android TV** → preselezionato **Schermo**;
- se la apri su un **telefono/tablet** → preselezionato **Tabellone**.

Puoi confermare o cambiare, poi premi **Continua**. Le opzioni sono tre:

- **Tabellone (comandi)** — forza sempre il ruolo controller.
- **Schermo (solo visualizzazione)** — forza sempre il ruolo display.
- **Automatico** — decide ad ogni avvio in base al dispositivo.

**In seguito** puoi cambiare ruolo:

- sul **controller**: menu `…` → **Ruolo dispositivo…**;
- sullo **schermo**: **tieni premuto** sullo schermo per riaprire la scelta.

---

## 4. I modi per mostrare il tabellone su un TV

### A) App Android su chiavetta / Android TV — *"accendi e vai"* (consigliato)

Metti nell'HDMI del TV una **chiavetta o box Android TV** (es. Fire TV Stick,
box Android TV) con installata questa app in ruolo **Schermo**.

1. Accendi il TV sull'ingresso HDMI della chiavetta.
2. Sul telefono apri l'app (ruolo **Tabellone**).
3. Lo schermo **trova da solo** il tabellone sulla rete e lo mostra.

Nessun indirizzo da digitare: la scoperta è automatica (mDNS/NSD).

### B) Browser di una smart TV — nessuna app sul TV

Se il TV ha già un **browser** (molte smart TV), non serve installare nulla sul
TV.

1. Sul telefono (ruolo **Tabellone**) apri il menu `…` → **Indirizzo / QR per la
   TV**: compaiono l'indirizzo `http://IP-telefono:porta` e un **QR code**.
2. Sul browser del TV digita quell'indirizzo (oppure inquadra il QR con un
   telefono per aprirlo/condividerlo).
3. Compare il tabellone in sola visualizzazione.

È il jolly universale: qualsiasi dispositivo con un browser sulla stessa rete va
bene.

### C) TV collegata "a filo" al telefono/tablet controller

Se colleghi il **controller stesso** a un display esterno:

- via **cavo USB‑C → HDMI**, oppure
- via **Miracast / "Trasmetti schermo"** (se supportato),

l'app rileva il display esterno e vi mostra automaticamente il tabellone (senza
comandi), mentre sul telefono restano i comandi. Il cavo USB‑C→HDMI è il metodo
più **robusto** in assoluto: utile come piano B se la rete fa i capricci.

---

## 5. Quale rete usare

Serve che controller e schermo siano sulla **stessa rete locale**.

- **WiFi condiviso** (palestra/casa): è il caso migliore. Collega entrambi allo
  stesso WiFi e sei a posto. Non serve internet: tutto avviene in locale.
- **Hotspot del telefono**: se in palestra non c'è WiFi, puoi accendere
  l'**hotspot del telefono** controller e collegare la chiavetta/TV a
  quell'hotspot (una volta sola: resta memorizzato).
  - Nota: per limiti di Android, l'app **non può accendere l'hotspot da sola** in
    modo affidabile; lo accendi tu dalle impostazioni del telefono, poi apri
    l'app.

---

## 6. TV molto vecchia (solo SCART / senza HDMI)

Se il TV non ha nemmeno l'HDMI, aggiungi un **adattatore HDMI → SCART/composito**
(pochi euro) tra la chiavetta (o il cavo) e il TV. Il resto non cambia.

---

## 7. Se qualcosa non va

- **Lo schermo non trova il tabellone** (scoperta automatica fallita): sullo
  schermo compare la richiesta di **inserire l'indirizzo a mano**
  (`IP:porta`, es. `192.168.1.20:8080`), lo stesso mostrato dal controller in
  `…` → **Indirizzo / QR per la TV**.
- **Il tabellone non parte / porta occupata**: l'app prova più porte
  automaticamente (8080, 8081, 8090, 8123, 8888). L'indirizzo effettivo è quello
  mostrato dal controller.
- **La scoperta automatica è instabile** su alcune reti: alcuni router bloccano
  il traffico multicast tra dispositivi. In quel caso usa l'indirizzo manuale
  (metodo B) o l'hotspot del telefono.
- **Immagine a scatti su chiavette molto economiche**: preferisci il cavo
  USB‑C→HDMI (metodo C) oppure una chiavetta un po' più prestante.

---

## 8. Riepilogo delle combinazioni

| Controller (comandi) | Schermo (visualizzazione) | Funziona? | Come |
|---|---|---|---|
| **App Android** | **App Android** (chiavetta/Android TV) | ✅ | Scoperta automatica |
| **App Android** | **Browser** (smart TV / altro dispositivo) | ✅ | Indirizzo/QR |
| **App Android** | **Display HDMI/Miracast** del controller stesso | ✅ | Automatico |
| **Browser (PWA)** | qualsiasi | ❌ | Un browser non può fare da controller |

**Regola d'oro:** *il controller è sempre l'app Android; lo schermo può essere
app o browser.*

---

*Autore: Daniele Lolli (UncleDan) — Tabellone Baskin Cast.*
