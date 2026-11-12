# Design — look soft (grigi), effetto CRT, modalità Carson

Data: 2026-11-12 · Progetto: Minimal Launcher Monospace (launcher Android text-only)

## Intent e perché

Tre nuove funzionalità, discusse e raffinate insieme nel brainstorming:

1. **Look soft nei grigi** — oggi il launcher è bianco puro su nero puro. Si vuole una versione più morbida, sempre dentro le sfumature di grigio, selezionabile dalle impostazioni. Lo sfondo resta molto molto scuro: l'ammorbidimento avviene quasi tutto sul bianco del testo. Niente tinte calde (niente "panna"/crema): solo grigi neutri.
2. **Effetto CRT leggero** — i pixel bianchi invadono leggermente i pixel scuri circostanti (sbavatura luminosa), più una griglia di righe di scansione orizzontali e verticali. Su tutto il launcher.
3. **Modalità "sdc"/"sdcr"** (sorry david carson / + rotated) — sulla sola pagina principale, una tipografia caotica: il testo riempie tutto lo spazio orizzontale, lettere maiuscole/minuscole casuali, lettere sostituite da varianti "sbagliate" con diacritici, spaziature irregolari; la variante sdcr aggiunge lettere capovolte verticalmente. Il caos si rimescola a ogni sblocco e resta fermo mentre lo guardi.

## Goals

- Quattro livelli di tema selezionabili nelle impostazioni (duro + tre grigi), che toccano tutto il launcher.
- Sbavatura luminosa: interruttore acceso/spento, solo intensità "leggera", predefinita spenta.
- Griglia di scansione: nessuna / sottile / marcata, predefinita nessuna.
- Modalità Carson: off / sdc / sdcr nelle impostazioni, effetto solo sulla pagina principale (header + righe delle app), predefinita off.
- Le interazioni restano identiche ovunque (tap per aprire, scroll della lista, gesture, ricerca).

## Non-goals

- Nessun colore: tutto resta in scala di grigi neutri.
- Niente vignettatura né curvatura da monitor (proposte nei mockup, scartate).
- Niente animazione del caos mentre la pagina è sotto gli occhi.
- La lista app, le impostazioni e il picker preferiti non ricevono il trattamento Carson.
- Nessun cambiamento ai meccanismi esistenti (favoriti, contatori, wallpaper, gesture).

## Anti-goals (cosa lo farebbe fallire anche se tecnicamente funziona)

- Leggibilità sotto i minimi: il testo primario di ogni livello deve restare sopra il minimo WCAG AA (4,5:1); il livello più spinto (#8A8A8A su #060606) è a ~5,9:1 e passa. Il criterio vale per il tema a griglia spenta: la griglia CRT scurisce di proposito i pixel che copre, come nel mockup approvato. Il testo secondario (Gray) è esente dal minimo, come oggi (#666 è già sotto 4,5:1 per scelta).
- Il caos Carson che sposta i testi fuori dalla loro riga: la griglia resta rispettata, ogni testo resta nella sua riga.
- Un tema "caldo" o color crema: respinto esplicitamente, non deve riapparire.
- Un effetto CRT che ruba i tocchi o disturba la lettura delle impostazioni.

## Vincoli del codice esistente (ricognizione fatta)

- **Jetpack Compose** senza Material: testo ovunque con `BasicText` + `monoStyle(fontSize, color, lineHeightFactor)` (`ui/theme/Theme.kt`); colori attuali `Black` #000, `White` #FFF, `Gray` #666.
- Font pixel monospace a spaziatura fissa (`undefined medium`): l'avanzamento di ogni glifo è prevedibile, utile per calcolare la dimensione che riempie la riga.
- `minSdk 26` → niente `RenderEffect` (API 31+) come unica strada.
- Impostazioni: DataStore con enum a scelta singola (`TimeSeparator`, `DateFormat`, …), flussi combinati in `LauncherViewModel` (pattern combine a due stadi, il vararg si ferma a 5), schermata a `LazyColumn` con `SectionLabel` + `ChoiceRow`.
- Schermate: `HomeScreen` (header + favoriti, nessuno scroll), `AppListScreen`, `SettingsScreen`, `FavoritesScreen`. La root è in `MainActivity.setContent`.
- Sblocco: oggi non esiste un rilevamento; `MainActivity` ha già `ON_RESUME` + `onNewIntent` per il tasto Home.

## Le decisioni prese insieme (non modificabili senza parlarne)

1. **Tema — 4 livelli** (scelta singola nelle impostazioni):
   - `DURO` — testo #FFFFFF, sfondo #000000 (oggi, predefinito)
   - `SOFT_1` — #C2C2C2 su #0A0A0A
   - `SOFT_2` — #A5A5A5 su #080808
   - `SOFT_3` — #8A8A8A su #060606
   Lo sfondo resta quasi nero in tutti i livelli: si ammorbidisce il testo, non lo sfondo.
2. **Sbavatura CRT** — interruttore, predefinito spento; acceso = solo intensità "leggera" (alone morbido, come text-shadow 0 0 2px + 0 0 4px del mockup approvato).
3. **Griglia CRT** — 3 opzioni: `NESSUN` (predefinita) / `SOTTILE` (1 px ogni 3 px) / `MARCATA` (2 px ogni 5 px), righe **sia orizzontali che verticali**.
4. **L'effetto CRT copre tutto il launcher**: pagina principale, lista app, impostazioni, picker preferiti.
5. **Carson** — modalità `OFF` / `SDC` / `SDCR`, solo pagina principale, **header compreso** (orologio, data, batteria, uso, sblocchi, notifiche).
6. **Il caos Carson è dentro la griglia**: ogni testo resta nella sua riga; la riga riempie l'orizzontale giocando con la dimensione del font; il caos sta in dimensione, carattere e spaziatura — maiuscole/minuscole casuali, lettere sostituite da varianti "sbagliate" (una `a` può diventare una qualsiasi di `A Á Ă Â Ä À Ā Ą Å Ã Æ`, maiuscola o minuscola — vale per ogni lettera), spaziature irregolari. `SDCR` aggiunge lettere capovolte verticalmente (scalaY −1 su lettere scelte a caso).
7. **Rimescolamento**: il caos si rigenera a ogni sblocco del telefono e resta identico finché lo si guarda.
8. **Tutto sempre dentro**: la dimensione delle righe si adatta perché header + tutte le righe dei preferiti entrino sempre nello schermo; il riempimento orizzontale è il massimo che ci sta, non il massimo possibile. Le proporzioni tra le righe restano caotiche: se l'insieme non ci sta, le righe si riducono tutte in scala (proporzionalmente) e la spaziatura assorbe la larghezza residua.
9. **Header Carson**: l'header resta nell'impaginazione a due colonne di oggi (orologio+data a sinistra, batteria/uso/sblocchi/notifiche a destra); il caos agisce dentro le colonne, senza spostare contenuti né tap target (orologio → Clock, contatori → Digital Wellbeing).

## Le mie chiamate (reversibili, segnate per il controllo)

- **Predefiniti**: tema `DURO`, sbavatura spenta, griglia `NESSUN`, Carson `OFF` — il launcher all'installazione resta com'è oggi.
- **Colore secondario `Gray`** (oggi #666 usato per orologio/data/crediti/voci non selezionate): il livello `DURO` tiene esattamente il #666 di oggi (all'installazione il launcher è identico a prima); i tre livelli soft derivano il loro secondario dal colore del testo (~60% di luminanza), così la gerarchia si conserva.
- **Wallpaper**: resta nero puro a prescindere dal tema (il launcher disegna il proprio sfondo; il wallpaper si vede solo negli scorci di sistema).
- **Marker dei favoriti** (⁵, ⁂, ⟶, …) in modalità Carson: restano normali alla loro dimensione, il caos tocca solo il nome; il marker non partecipa al calcolo del riempimento (la riga riempie la larghezza rimanente); il suggerimento "favoriti vuoti" resta normale.
- **Pixel dei mockup = dp sul dispositivo**: i valori in px dei mockup approvati (griglia 1/3 e 2/5, sbavatura 2+4) si intendono come dp, convertiti con la densità dello schermo, così l'effetto ha le stesse proporzioni viste nel browser; il costruttore può ritoccare il passo della griglia se sul font pixel produce moiré.
- **Cifre** (orologio, percentuali): non hanno varianti con diacritici — il loro caos si gioca su capovolgimenti (solo in sdcr), salti di dimensione e spaziature irregolari.
- **Jitter per-lettera della dimensione**: lieve (±15%) in entrambe le modalità, in aggiunta alla dimensione di riga che riempie la larghezza.
- **Rilevamento sblocco**: broadcast `ACTION_USER_PRESENT` (scatta a ogni sblocco, anche biometrico, e non a ogni screen-on).
- **Sbavatura**: ombra di testo Compose (`Shadow`) sul colore del testo, funziona da API 26; niente blur di layer (richiederebbe API 31+ o un doppio percorso).
- **Griglia**: overlay disegnato sopra tutto il contenuto con Canvas, che non intercetta i tocchi.

## Lasciato al costruttore (da decidere nel piano, senza tornare dall'autore)

- La strategia esatta per distribuire i colori del tema (CompositionLocal vs parametri espliciti — il codice oggi usa parametri espliciti con default) e per l'ombra globale.
- Come disegnare le lettere Carson (Layout custom con `graphicsLayer` per rotazione/capovolgimento, oppure Canvas con disegno per-grafo): deve restare cliccabile l'intera riga.
- La tabella di varianti per lettera (quali diacritici includere per ogni lettera, incluse maiuscole/minuscole).
- L'algoritmo "sta tutto sempre": ripartizione delle altezze tra le righe, riduzione proporzionale in caso di overflow, assorbimento della larghezza residua con la spaziatura.
- Organizzazione delle nuove voci nelle impostazioni (sezioni e stringhe, in inglese come le esistenti).
- La derivazione precisa del `Gray` secondario per livello.
- Test.
