# Look soft (grigi) + effetto CRT + modalità Carson — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Aggiungere al launcher tre funzionalità selezionabili dalle impostazioni: 4 livelli di tema in scala di grigi, effetto CRT (sbavatura luminosa + griglia di scansione) su tutto il launcher, e due modalità tipografiche caotiche "sdc"/"sdcr" per la sola pagina principale.

**Architecture:** Tutta la logica calcolabile (tavolozze, costanti CRT, generazione del caos Carson, calcolo di riempimento/adattamento) vive in oggetti puri Kotlin testabili con JUnit; la UI Compose resta un sottile strato di collegamento. Il tema viaggia via `CompositionLocal` fornito in `MainActivity`; l'effetto CRT è un overlay Canvas sopra tutte le schermate + un'ombra di testo opzionale; il Carson è un `Layout` custom che misura l'header e piazza lettera per lettera le righe dei preferiti.

**Tech Stack:** Kotlin, Jetpack Compose (BOM 2025.06.01, niente Material), DataStore Preferences, JUnit 4 (da aggiungere), Gradle 8.13/AGP 8.13.2, minSdk 26 / compileSdk 36.

**Spec:** `docs/superpowers/specs/2026-11-12-look-and-carson-design.md`

## Global Constraints

- **Nessun commit git**: tutte le modifiche restano nel working tree finché l'utente non valida sul dispositivo (istruzione esplicita dell'utente). Ogni task termina con la verifica, mai con un commit.
- minSdk 26, compileSdk 36, Kotlin 2.4.20, Compose BOM 2025.06.01: niente API sopra la 26 senza fallback (es. niente `RenderEffect`).
- Niente Material: solo `BasicText`/`BasicTextField`/`monoStyle`; lo stile esistente (parametri espliciti, `lineHeightFactor = 1.25f`, `includeFontPadding = false`) si conserva.
- Nuove impostazioni = enum con `label` (pattern `TimeSeparator`); il default di ogni flusso DataStore è `entries.first()`, quindi il primo elemento dell'enum è il predefinito.
- Stringhe UI in inglese, in `res/values/strings.xml`.
- Palette approvata (valori esatti): DURO `#FFFFFF`/`#000000` (secondario `#666666` invariato); SOFT_1 `#C2C2C2`/`#0A0A0A`; SOFT_2 `#A5A5A5`/`#080808`; SOFT_3 `#8A8A8A`/`#060606`; secondario dei soft = interpolazione canale-per-canale sfondo→testo a t=0,6 (formula `blendARGB`).
- Font pixel a spaziatura fissa: avanzamento glifo = `0,6 × fontSize` (costante `MONO_ADVANCE`).
- Test: JUnit 4, run con `./gradlew testDebugUnitTest`.

## Review Focus

1. **8 preferiti con nomi cortissimi o lunghissimi** devono stare sempre nello schermo, senza scroll né tagli (la regola "sta tutto sempre"). → Test in Task 4: 8 etichette corte con budget piccolo → tutte le righe con dimensioni > 0 e somma ≤ budget.
2. **Ritorno alla home senza sblocco** (apri un'app, torni): la composizione deve essere identica a prima, perché il seed dipende solo da (epoch, indice, etichetta). → Test in Task 3: stesso seed → stesso piano, lettera per lettera.
3. **Favorito con marker** (⁵, ⁂…): il marker resta leggibile e il nome riempie solo la larghezza rimanente. → Test in Task 4: il piano esclude `markerWidthDp` e la larghezza risultante ≈ `availableWidthDp − markerWidthDp`.
4. **Cifre e simboli** (orologio, `%`, `_`, `≡`): non ricevono mai varianti diacritiche, solo dimensione/spaziatura/capovolgimenti (sdcr). → Test in Task 3: `"2"`, `"%23_45"` → glifi invariati.
5. **Modalità Carson con zero preferiti**: il suggerimento "no favorite apps" resta normale e leggibile. → Task 8, passo esplicito (il ramo hint non cambia in modalità Carson).

---

### Task 1: Tavolozza dei temi (ThemeLevel + LauncherPalette)

**Files:**
- Create: `app/src/main/java/com/riccardopatane/minimallauncher/model/ThemeLevel.kt`
- Modify: `app/src/main/java/com/riccardopatane/minimallauncher/ui/theme/Theme.kt`
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts` (infra JUnit)
- Test: `app/src/test/java/com/riccardopatane/minimallauncher/ui/theme/PaletteTest.kt`

**Interfaces:**
- Consumes: niente (primo task).
- Produces:
  - `enum class ThemeLevel(val label: String) { DURO("duro"), SOFT_1("soft 1"), SOFT_2("soft 2"), SOFT_3("soft 3") }` — primo elemento = default.
  - `data class LauncherPalette(val text: Color, val background: Color, val secondary: Color)`
  - `fun paletteFor(level: ThemeLevel): LauncherPalette`
  - `fun blendARGB(from: Long, to: Long, fraction: Float): Long` — interpolazione per canale in spazio sRGB (come ArgbEvaluator).
  - `val LocalLauncherColors: ProvidableCompositionLocal<LauncherPalette>` = `staticCompositionLocalOf { paletteFor(ThemeLevel.DURO) }`

- [ ] **Step 1: Aggiungere JUnit al version catalog e al build** — in `libs.versions.toml`: `junit = "4.13.2"` + library `junit = { group = "junit", name = "junit", version.ref = "junit" }`; in `app/build.gradle.kts` dipendenze: `testImplementation(libs.junit)`.

- [ ] **Step 2: Scrivere i test che falliscono** in `PaletteTest.kt`

```kotlin
class PaletteTest {
    @Test fun duro_has_today_palette() {
        val p = paletteFor(ThemeLevel.DURO)
        assertEquals(Color(0xFF000000), p.background)
        assertEquals(Color(0xFFFFFFFF), p.text)
        assertEquals(Color(0xFF666666), p.secondary) // #666 di oggi, invariato
    }
    @Test fun soft_levels_have_approved_hexes() {
        assertEquals(Color(0xFF0A0A0A) to Color(0xFFC2C2C2), paletteFor(ThemeLevel.SOFT_1).let { it.background to it.text })
        assertEquals(Color(0xFF080808) to Color(0xFFA5A5A5), paletteFor(ThemeLevel.SOFT_2).let { it.background to it.text })
        assertEquals(Color(0xFF060606) to Color(0xFF8A8A8A), paletteFor(ThemeLevel.SOFT_3).let { it.background to it.text })
    }
    @Test fun soft_secondary_is_60_percent_toward_text() {
        assertEquals(Color(0xFF787878), paletteFor(ThemeLevel.SOFT_1).secondary)
        assertEquals(Color(0xFF666666), paletteFor(ThemeLevel.SOFT_2).secondary)
        assertEquals(Color(0xFF555555), paletteFor(ThemeLevel.SOFT_3).secondary)
    }
    @Test fun blendARGB_blends_each_channel() {
        assertEquals(0xFF787878L, blendARGB(0xFF0A0A0A, 0xFFC2C2C2, 0.6f))
        assertEquals(0xFF000000L, blendARGB(0xFF000000, 0xFFFFFFFF, 0f))
        assertEquals(0xFFFFFFFFL, blendARGB(0xFF000000, 0xFFFFFFFF, 1f))
    }
}
```

- [ ] **Step 3: Eseguire i test per vederli fallire**

Run: `./gradlew testDebugUnitTest`
Expected: compilazione fallita — `ThemeLevel`, `paletteFor`, `blendARGB` non esistono.

- [ ] **Step 4: Implementare** `ThemeLevel.kt` (enum con label, pattern `TimeSeparator`) e in `Theme.kt` `LauncherPalette`, `paletteFor` (DURO usa le costanti esistenti `Black`/`White`/`Gray`; i soft usano i valori esatti sopra e `secondary = Color(blendARGB(bg.value, text.value, 0.6f))`), `blendARGB` (media per canale ARGB di `from`/`to` in base a `fraction`), `LocalLauncherColors`.

- [ ] **Step 5: Eseguire i test e vederli passare**

Run: `./gradlew testDebugUnitTest`
Expected: 4 test PASS.

### Task 2: Costanti dell'effetto CRT (CrtGrid + CrtSpec)

**Files:**
- Create: `app/src/main/java/com/riccardopatane/minimallauncher/model/CrtGrid.kt`
- Create: `app/src/main/java/com/riccardopatane/minimallauncher/ui/crt/CrtSpec.kt`
- Test: `app/src/test/java/com/riccardopatane/minimallauncher/ui/crt/CrtSpecTest.kt`

**Interfaces:**
- Consumes: niente.
- Produces:
  - `enum class CrtGrid(val label: String) { NONE("none"), THIN("thin"), BOLD("bold") }` — primo elemento = default.
  - `object CrtSpec` con costanti: `GLOW_BLUR_DP = 4f`, `THIN_LINE_DP = 1f`, `THIN_PERIOD_DP = 3f`, `THIN_ALPHA = 0.18f`, `BOLD_LINE_DP = 2f`, `BOLD_PERIOD_DP = 5f`, `BOLD_ALPHA = 0.30f`.

- [ ] **Step 1: Scrivere i test che falliscono** in `CrtSpecTest.kt`

```kotlin
class CrtSpecTest {
    @Test fun glow_blur_is_4dp() { assertEquals(4f, CrtSpec.GLOW_BLUR_DP) }
    @Test fun thin_grid_is_1px_every_3px_at_18_percent() {
        assertEquals(1f, CrtSpec.THIN_LINE_DP)
        assertEquals(3f, CrtSpec.THIN_PERIOD_DP)
        assertEquals(0.18f, CrtSpec.THIN_ALPHA)
    }
    @Test fun bold_grid_is_2px_every_5px_at_30_percent() {
        assertEquals(2f, CrtSpec.BOLD_LINE_DP)
        assertEquals(5f, CrtSpec.BOLD_PERIOD_DP)
        assertEquals(0.30f, CrtSpec.BOLD_ALPHA)
    }
    @Test fun none_is_first_entry_and_default() {
        assertEquals(CrtGrid.NONE, CrtGrid.entries.first())
    }
}
```

- [ ] **Step 2: Eseguire i test per vederli fallire**

Run: `./gradlew testDebugUnitTest`
Expected: compilazione fallita.

- [ ] **Step 3: Implementare** `CrtGrid.kt` e `CrtSpec.kt` con i valori esatti sopra.

- [ ] **Step 4: Eseguire i test e vederli passare**

Run: `./gradlew testDebugUnitTest`
Expected: 4 test PASS.

### Task 3: Motore Carson — varianti, semi e piani per lettera

**Files:**
- Create: `app/src/main/java/com/riccardopatane/minimallauncher/model/CarsonMode.kt`
- Create: `app/src/main/java/com/riccardopatane/minimallauncher/ui/carson/CarsonEngine.kt`
- Test: `app/src/test/java/com/riccardopatane/minimallauncher/ui/carson/CarsonEngineTest.kt`

**Interfaces:**
- Consumes: niente.
- Produces:
  - `enum class CarsonMode(val label: String) { OFF("off"), SDC("sdc"), SDCR("sdcr") }` — primo elemento = default.
  - `const val MONO_ADVANCE = 0.6f`
  - `val VARIANT_MAP: Map<Char, List<Char>>` — chiave = lettera base minuscola a–z; valori = maiuscola/minuscola base + varianti diacritiche (es. `'a'` → `AaÁĂÂÄÀĀĄÅÃÆáăâäàāąåãæ`); lettere fuori mappa → lista vuota.
  - `data class LetterSpec(val glyph: String, val sizeFactor: Float, val flipped: Boolean, val spacingDp: Float)`
  - `data class RowPlan(val letters: List<LetterSpec>, val fontSizeSp: Float)`
  - `fun carsonSeed(epoch: Int, index: Int, label: String): Long` — `(epoch.toLong() * 31 + index) * 31 + label.hashCode()`
  - `fun computeLinePlan(text: String, mode: CarsonMode, seed: Long, baseSizeSp: Float): RowPlan` — per le righe dell'header: `fontSizeSp = baseSizeSp`, per lettera: `sizeFactor ∈ [0.85, 1.15]`, `spacingDp` irregolare in `[0, baseSizeSp * 0.4]`, `flipped` solo in SDCR con probabilità 0,25, varianti solo per lettere.
  - Regole di generazione (valgono per tutti i piani): `Random(seed)`; per ogni carattere: se è una lettera → glyph scelto a caso da `VARIANT_MAP[base.lowercase()]` (maiuscole/minuscole incluse), altrimenti glyph = carattere invariato; `flipped` solo se `mode == SDCR`; mai `mode == OFF` in ingresso (la UI non chiama il motore con OFF).

- [ ] **Step 1: Scrivere i test che falliscono** in `CarsonEngineTest.kt`

```kotlin
class CarsonEngineTest {
    @Test fun same_seed_same_plan() {
        val a = computeLinePlan("WhatsApp", CarsonMode.SDCR, 42L, 20f)
        val b = computeLinePlan("WhatsApp", CarsonMode.SDCR, 42L, 20f)
        assertEquals(a, b)
    }
    @Test fun different_seeds_differ() {
        val a = computeLinePlan("WhatsApp", CarsonMode.SDC, 1L, 20f)
        val b = computeLinePlan("WhatsApp", CarsonMode.SDC, 2L, 20f)
        assertNotEquals(a.letters.map { it.glyph }, b.letters.map { it.glyph })
    }
    @Test fun flips_only_in_sdcr() {
        val plain = computeLinePlan("WhatsApp", CarsonMode.SDC, 7L, 20f)
        val rot = computeLinePlan("WhatsApp", CarsonMode.SDCR, 7L, 20f)
        assertTrue(plain.letters.none { it.flipped })
        // con 8 lettere e p=0.25, almeno un capovolgimento è pressoché certo; il
        // test fissa un seme noto e verifica che il capovolgimento avvenga
        assertTrue(rot.letters.any { it.flipped })
    }
    @Test fun letters_get_variants_but_digits_and_symbols_do_not() {
        val plan = computeLinePlan("23_45%", CarsonMode.SDCR, 9L, 18f)
        assertEquals(listOf("2", "3", "_", "4", "5", "%"), plan.letters.map { it.glyph })
    }
    @Test fun letter_variants_come_from_the_map() {
        val plan = computeLinePlan("Aa", CarsonMode.SDC, 3L, 20f)
        plan.letters.forEach { spec ->
            val base = spec.glyph.lowercase()[0]
            assertTrue(VARIANT_MAP.getValue(base).contains(spec.glyph[0]))
        }
    }
    @Test fun size_factor_and_spacing_stay_in_range() {
        val plan = computeLinePlan("Impostazioni", CarsonMode.SDC, 11L, 56f)
        plan.letters.forEach {
            assertTrue(it.sizeFactor in 0.85f..1.15f)
            assertTrue(it.spacingDp in 0f..(56f * 0.4f))
        }
    }
}
```

- [ ] **Step 2: Eseguire i test per vederli fallire**

Run: `./gradlew testDebugUnitTest`
Expected: compilazione fallita — `CarsonMode`/`CarsonEngine` non esistono.

- [ ] **Step 3: Implementare** `CarsonMode.kt` e `CarsonEngine.kt`. Il costruttore dei piani è una funzione privata `lettersFor(text, mode, random, baseSizeSp, spacingMax)` condivisa dal riempimento (Task 4). NB il test del seme `7L` per i flips: verificare che il seme scelto produca davvero un flip (altrimenti fissare un altro seme nel test — il comportamento resta quello specificato).

- [ ] **Step 4: Eseguire i test e vederli passare**

Run: `./gradlew testDebugUnitTest`
Expected: 6 test PASS.

### Task 4: Motore Carson — riempimento riga e "sta tutto sempre"

**Files:**
- Modify: `app/src/main/java/com/riccardopatane/minimallauncher/ui/carson/CarsonEngine.kt`
- Test: `app/src/test/java/com/riccardopatane/minimallauncher/ui/carson/CarsonFitTest.kt`

**Interfaces:**
- Consumes: da Task 3 — `LetterSpec`, `RowPlan`, `MONO_ADVANCE`, `carsonSeed`, `lettersFor`.
- Produces:
  - `const val ROW_VERTICAL_PADDING_DP = 16f` (8dp sopra + 8dp sotto, come `AppRow`), `const val ROW_LINE_HEIGHT_FACTOR = 1.25f`, `const val MARKER_SIZE_SP = 20f`
  - `fun markerWidthDp(marker: String): Float` — `marker.length * MONO_ADVANCE * MARKER_SIZE_SP` (0 per marker vuoto).
  - `fun computeRowPlan(text: String, mode: CarsonMode, seed: Long, availableWidthDp: Float, maxFontSizeSp: Float? = null): RowPlan` — dimensione di riempimento: `fontSizeSp = (availableWidthDp − spacing totale target) / (len × MONO_ADVANCE)`; se `maxFontSizeSp` è dato e la dimensione lo supera, si usa `maxFontSizeSp` e la spaziatura assorbe la larghezza residua; `spacingDp` per lettera = base uniforme con jitter ±40% (mai negativa).
  - `fun computeFavoritesPlan(labels: List<String>, mode: CarsonMode, epoch: Int, availableWidthDp: Float, heightBudgetDp: Float): List<RowPlan>` — algoritmo "sta tutto sempre": 1) dimensione naturale di ogni riga = riempimento larghezza (senza marker: il chiamante passa la larghezza già al netto del marker); 2) altezza naturale = `size × 1.25 + ROW_VERTICAL_PADDING_DP`; 3) se la somma ≤ budget → dimensioni naturali; 4) altrimenti `scale = budget / somma`, ogni riga prende altezza `naturale × scale` e quindi `size = (altezza − 16) / 1.25`, con spaziatura che riporta la riga a piena larghezza. Semi: `carsonSeed(epoch, index, label)`.

- [ ] **Step 1: Scrivere i test che falliscono** in `CarsonFitTest.kt`

```kotlin
class CarsonFitTest {
    @Test fun row_fills_the_available_width() {
        val plan = computeRowPlan("WhatsApp", CarsonMode.SDC, 5L, availableWidthDp = 300f)
        val width = plan.letters.sumOf { it.spacingDp } +
            plan.letters.sumOf { MONO_ADVANCE * plan.fontSizeSp * it.sizeFactor }
        assertEquals(300f, width, 0.5f)
    }
    @Test fun max_size_caps_and_spacing_absorbs() {
        val plan = computeRowPlan("a", CarsonMode.SDC, 5L, availableWidthDp = 300f, maxFontSizeSp = 80f)
        assertEquals(80f, plan.fontSizeSp, 0.001f)
        val width = plan.letters.sumOf { it.spacingDp } + plan.letters.sumOf { MONO_ADVANCE * plan.fontSizeSp * it.sizeFactor }
        assertEquals(300f, width, 0.5f)
    }
    @Test fun marker_width_is_excluded_by_the_caller_but_predictable() {
        assertEquals(0f, markerWidthDp(""))
        assertEquals("⟶".length * MONO_ADVANCE * 20f, markerWidthDp("⟶"))
    }
    @Test fun eight_short_labels_always_fit_the_budget() {
        val labels = listOf("a", "b", "c", "d", "e", "f", "g", "h")
        val plans = computeFavoritesPlan(labels, CarsonMode.SDCR, epoch = 3, availableWidthDp = 340f, heightBudgetDp = 400f)
        assertEquals(8, plans.size)
        val total = plans.sumOf { it.fontSizeSp * 1.25f + 16f }
        assertTrue(total <= 400f + 0.5f)
        plans.forEach { assertTrue(it.fontSizeSp > 0f) }
    }
    @Test fun overflow_scales_all_rows_proportionally() {
        val plans = computeFavoritesPlan(listOf("WhatsApp", "Telegram", "Gmail"), CarsonMode.SDC, epoch = 1, availableWidthDp = 340f, heightBudgetDp = 60f)
        val total = plans.sumOf { it.fontSizeSp * 1.25f + 16f }
        assertTrue(total <= 60f + 0.5f)
        // altezze naturali pre-scala (dimensione di riempimento × 1.25 + 16)
        val naturals = listOf("WhatsApp", "Telegram", "Gmail").map { 340f / (it.length * MONO_ADVANCE) }
        val natHeights = naturals.map { it * 1.25f + 16f }
        // ogni riga è stata ridotta con lo STESSO fattore di scala
        val ratioA = (plans[0].fontSizeSp * 1.25f + 16f) / natHeights[0]
        val ratioB = (plans[1].fontSizeSp * 1.25f + 16f) / natHeights[1]
        val ratioC = (plans[2].fontSizeSp * 1.25f + 16f) / natHeights[2]
        assertEquals(ratioA, ratioB, 0.01f)
        assertEquals(ratioA, ratioC, 0.01f)
    }
    @Test fun same_epoch_same_plans_across_calls() {
        val a = computeFavoritesPlan(listOf("Maps", "Note"), CarsonMode.SDC, epoch = 9, 340f, 400f)
        val b = computeFavoritesPlan(listOf("Maps", "Note"), CarsonMode.SDC, epoch = 9, 340f, 400f)
        assertEquals(a, b)
    }
}
```

- [ ] **Step 2: Eseguire i test per vederli fallire**

Run: `./gradlew testDebugUnitTest`
Expected: compilazione fallita (funzioni assenti) o FAIL.

- [ ] **Step 3: Implementare** `markerWidthDp`, `computeRowPlan`, `computeFavoritesPlan` in `CarsonEngine.kt`. Il jitter di spaziatura va applicato per ultimo e ri-normalizzato, così la riga resta a piena larghezza (o assorbe il residuo nel caso `maxFontSizeSp`).

- [ ] **Step 4: Eseguire i test e vederli passare**

Run: `./gradlew testDebugUnitTest`
Expected: 6 test PASS.

### Task 5: Impostazioni — chiavi, repository, ViewModel, schermata

**Files:**
- Modify: `app/src/main/java/com/riccardopatane/minimallauncher/data/SettingsRepository.kt`
- Modify: `app/src/main/java/com/riccardopatane/minimallauncher/ui/LauncherViewModel.kt`
- Modify: `app/src/main/java/com/riccardopatane/minimallauncher/ui/SettingsScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: da Task 1 — `ThemeLevel`, `LocalLauncherColors` (non ancora usato qui); da Task 2 — `CrtGrid`; da Task 3 — `CarsonMode`.
- Produces (per i task 6–8):
  - Repository: `themeLevel: Flow<ThemeLevel>`, `crtGrid: Flow<CrtGrid>`, `crtGlow: Flow<Boolean>`, `carsonMode: Flow<CarsonMode>` + `suspend fun setThemeLevel/setCrtGrid/setCrtGlow/setCarsonMode`. Chiavi: `theme_level`, `crt_grid`, `crt_glow`, `carson_mode`; default = `entries.first()` (e `false` per crtGlow), con il pattern `firstOrNull { it.name == name }`.
  - `LauncherUiState` guadagna: `themeLevel: ThemeLevel = ThemeLevel.DURO`, `crtGrid: CrtGrid = CrtGrid.NONE`, `crtGlow: Boolean = false`, `carsonMode: CarsonMode = CarsonMode.OFF`. Catene combine (limite 5): `settingsBase2 = combine(settingsBase, favoritePosition, showUnlocks, showNotifications, themeLevel)` e `settingsState = combine(settingsBase2, crtGrid, crtGlow, carsonMode)`.
  - ViewModel: `setThemeLevel`, `setCrtGrid`, `setCrtGlow`, `setCarsonMode` (pattern dei setter esistenti).
  - `SettingsScreen`: tre nuove sezioni dopo "home info": `settings_look` → `ThemeLevel.entries` in `ChoiceRow`; `settings_crt` → `ChoiceRow` "glow" (selected = crtGlow, onClick = setCrtGlow(!crtGlow)) + `CrtGrid.entries` in `ChoiceRow`; `settings_typography` → `CarsonMode.entries` in `ChoiceRow`.
  - strings.xml: `settings_look` = "look", `settings_crt` = "crt effect", `settings_crt_glow` = "glow", `settings_typography` = "typography".

- [ ] **Step 1: Aggiungere chiavi, flussi e setter** in `SettingsRepository.kt` (pattern identico a `dateFormat`/`timeSeparator`).

- [ ] **Step 2: Estendere `LauncherUiState`, le catene combine e i setter** in `LauncherViewModel.kt`.

- [ ] **Step 3: Aggiungere le stringhe** in `strings.xml`.

- [ ] **Step 4: Aggiungere le tre sezioni** in `SettingsScreen.kt` con `SectionLabel`/`ChoiceRow` esistenti.

- [ ] **Step 5: Verificare la compilazione**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

### Task 6: Applicare il tema a tutte le schermate

**Files:**
- Modify: `app/src/main/java/com/riccardopatane/minimallauncher/MainActivity.kt` (provider CompositionLocal)
- Modify: `app/src/main/java/com/riccardopatane/minimallauncher/ui/theme/Theme.kt` (helper `launcherStyle`)
- Modify: `app/src/main/java/com/riccardopatane/minimallauncher/ui/HomeScreen.kt`, `AppListScreen.kt`, `SettingsScreen.kt`, `FavoritesScreen.kt`, `ui/components/HeaderBlock.kt`, `AppRow.kt`, `SearchField.kt`
- Test: nessun nuovo test (passata meccanica) — gate = build.

**Interfaces:**
- Consumes: da Task 5 — `uiState.themeLevel`; da Task 1 — `paletteFor`, `LocalLauncherColors`.
- Produces:
  - In `Theme.kt`: `@Composable fun launcherStyle(fontSize: TextUnit, color: Color, lineHeightFactor: Float = 1.25f): TextStyle` = `monoStyle(fontSize, color, lineHeightFactor)` per ora (l'ombra arriva nel Task 7).
  - In `MainActivity.setContent`: `val state by viewModel.uiState.collectAsStateWithLifecycle()`; tutto il contenuto dentro `CompositionLocalProvider(LocalLauncherColors provides paletteFor(state.themeLevel)) { … }`.
  - Convenzione: in ogni schermata `val colors = LocalLauncherColors.current`; `background(Black)` → `background(colors.background)`; `White` → `colors.text`; `Gray` → `colors.secondary`; le chiamate `monoStyle(…)` senza colore esplicito → `launcherStyle(…, colors.text)`; il cursor del `SearchField` → `colors.text`.

- [ ] **Step 1: Provider in `MainActivity`** — come sopra, senza cambiare la struttura delle schermate.

- [ ] **Step 2: `launcherStyle` in `Theme.kt`.**

- [ ] **Step 3: Passata su `HomeScreen.kt`** — background, hint favoriti (`colors.secondary`), righe via `AppRow` (il colore default arriva da `launcherStyle` in `AppRow`).

- [ ] **Step 4: Passata su `AppRow.kt`** — `val colors = LocalLauncherColors.current`, `style = launcherStyle(20.sp, colors.text)`.

- [ ] **Step 5: Passata su `HeaderBlock.kt`** — orologio/data/batteria/uso: `Gray` → `colors.secondary`.

- [ ] **Step 6: Passata su `AppListScreen.kt`** — background, `no results` → `colors.secondary`, righe via `AppRow`.

- [ ] **Step 7: Passata su `SearchField.kt`** — `textStyle = launcherStyle(20.sp, colors.text)`, `cursorBrush = SolidColor(colors.text)`, hint → `colors.secondary`.

- [ ] **Step 8: Passata su `SettingsScreen.kt`** — background, `ChoiceRow` (selezionato `colors.text`, no `colors.secondary`), `SectionLabel`/titoli `colors.text`, crediti `colors.secondary`.

- [ ] **Step 9: Passata su `FavoritesScreen.kt`** — background, back `colors.secondary`, titolo `colors.text`, righe preferite `colors.text`/`colors.secondary`.

- [ ] **Step 10: Verificare la compilazione**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL. (Il launcher funziona come oggi: il default DURO riproduce esattamente i colori attuali.)

### Task 7: Effetto CRT — ombra (glow) e griglia di scansione

**Files:**
- Modify: `app/src/main/java/com/riccardopatane/minimallauncher/ui/theme/Theme.kt` (parametro `shadow` su `monoStyle`)
- Create: `app/src/main/java/com/riccardopatane/minimallauncher/ui/crt/CrtGlow.kt`
- Create: `app/src/main/java/com/riccardopatane/minimallauncher/ui/crt/CrtOverlay.kt`
- Modify: `app/src/main/java/com/riccardopatane/minimallauncher/MainActivity.kt` (provider glow + overlay + Box)
- Modify: le chiamate `launcherStyle`/`monoStyle` di tutte le schermate (una passata meccanica: `launcherStyle` ora include l'ombra quando attiva)

**Interfaces:**
- Consumes: da Task 5 — `uiState.crtGlow`, `uiState.crtGrid`; da Task 2 — `CrtSpec`, `CrtGrid`.
- Produces:
  - `monoStyle(…, shadow: Shadow? = null)` — passa il parametro a `TextStyle`.
  - `val LocalCrtGlow = staticCompositionLocalOf { false }`
  - `@Composable fun crtGlowShadow(textColor: Color): Shadow?` — se `LocalCrtGlow.current`: `Shadow(color = textColor, blurRadius = CrtSpec.GLOW_BLUR_DP.dp.toPx())` (offset zero), altrimenti `null`.
  - `launcherStyle` aggiornato: `monoStyle(fontSize, color, lineHeightFactor, shadow = crtGlowShadow(color))`.
  - `@Composable fun CrtOverlay(grid: CrtGrid, modifier: Modifier = Modifier)` — `Canvas`: se THIN, righe verticali e orizzontali `1.dp` ogni `3.dp`, colore `Black` con alpha `0.18f`; se BOLD, `2.dp` ogni `5.dp`, alpha `0.30f`; se NONE non disegna nulla. Nessun `pointerInput`: non intercetta i tocchi.
  - In `MainActivity`: `CompositionLocalProvider(LocalLauncherColors …, LocalCrtGlow provides state.crtGlow) { Box(Modifier.fillMaxSize()) { <schermata>; CrtOverlay(state.crtGrid, Modifier.matchParentSize()) } }` — l'overlay è sopra TUTTE le schermate (anche settings e picker).

- [ ] **Step 1: `monoStyle` con `shadow` + `CrtGlow.kt` + `launcherStyle` aggiornato.**

- [ ] **Step 2: `CrtOverlay.kt`** — Canvas con i valori di `CrtSpec` convertiti via `LocalDensity`.

- [ ] **Step 3: Provider e overlay in `MainActivity`.**

- [ ] **Step 4: Verificare la compilazione**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Verificare l'isolamento dell'overlay** — rileggere `CrtOverlay`: nessun modificatore di input; la UI sotto resta interamente cliccabile (nota per la validazione manuale del Task 9).

### Task 8: Modalità Carson sulla pagina principale

**Files:**
- Create: `app/src/main/java/com/riccardopatane/minimallauncher/ui/carson/CarsonText.kt`
- Create: `app/src/main/java/com/riccardopatane/minimallauncher/ui/carson/CarsonFavoritesLayout.kt`
- Modify: `app/src/main/java/com/riccardopatane/minimallauncher/ui/HomeScreen.kt`
- Modify: `app/src/main/java/com/riccardopatane/minimallauncher/ui/components/HeaderBlock.kt`
- Modify: `app/src/main/java/com/riccardopatane/minimallauncher/MainActivity.kt` (receiver sblocco)
- Modify: `app/src/main/java/com/riccardopatane/minimallauncher/ui/LauncherViewModel.kt` (epoch)

**Interfaces:**
- Consumes: da Task 3/4 — `CarsonMode`, `RowPlan`, `computeLinePlan`, `computeFavoritesPlan`, `markerWidthDp`, `MONO_ADVANCE`; da Task 5 — `uiState.carsonMode`; da Task 6 — `LocalLauncherColors`, `launcherStyle`.
- Produces:
  - `@Composable fun CarsonText(plan: RowPlan, color: Color, modifier: Modifier = Modifier)` — `Layout` che misura ogni lettera con `BasicText(letter.glyph, style = launcherStyle((plan.fontSizeSp * letter.sizeFactor).sp, color, lineHeightFactor = 1.25f))` e la piazza in sequenza con `letter.spacingDp`; il flip si ottiene con `Modifier.graphicsLayer { scaleY = -1f }` sul figlio; nessuna rotazione (sdcr = solo capovolgimenti). Con il glow attivo, anche le lettere Carson ricevono l'ombra (via `launcherStyle`).
  - `@Composable fun CarsonFavoritesLayout(header: @Composable () -> Unit, labels: List<String>, markers: List<String>, mode: CarsonMode, epoch: Int, onLaunch: (Int) -> Unit, modifier: Modifier = Modifier)` — `Layout`: misura `header` (senza vincolo di altezza), `budget = maxHeight − headerHeight`; `availableWidthDp = maxWidth − 40.dp` (padding orizzontale di oggi); larghezza utile per riga = `availableWidthDp − markerWidthDp(marker)`; piani = `computeFavoritesPlan(…, availableWidthDp − markerWidth, budget)`; ogni riga è un figlio `Row` = marker `BasicText` (20.sp, `launcherStyle(20.sp, colors.text)`) + `CarsonText(plan, colors.text)`, `clickable { onLaunch(index) }`, padding verticale 8.dp (i 16.dp totali sono già nel piano). Posizione: TOP → righe sotto l'header; BOTTOM → righe allineate in basso (come lo `Spacer(weight)` di oggi).
  - `HomeScreen`: raccoglie `viewModel.carsonEpochFlow`; se `state.carsonMode != CarsonMode.OFF && state.favorites.isNotEmpty()` → `CarsonFavoritesLayout(header = { HeaderBlock(…, carsonMode, carsonEpoch) }, labels = favorites.map { it.label }, markers = <come oggi, stesso ordine>, mode, epoch, onLaunch = { viewModel.launch(state.favorites[it]) })`; altrimenti il Column di oggi. Il ramo "favoriti vuoti" (hint) resta IDENTICO anche in modalità Carson.
  - `HeaderBlock(…, carsonMode: CarsonMode = CarsonMode.OFF, carsonEpoch: Int = 0)` — se OFF: rendering attuale; altrimenti, dentro le STESSE due colonne e con gli STESSI `Modifier` cliccabili, ogni riga di testo diventa `CarsonText(computeLinePlan(text, mode, carsonSeed(carsonEpoch, i, text), baseSizeSp), colors.text)` con `baseSizeSp = 56f` per l'orologio e `18f` per tutte le altre righe; `i` = indice progressivo della riga nell'header (0 orologio, 1 data, 2 batteria, 3 uso, 4 sblocchi, 5 notifiche).
  - ViewModel: `private val carsonEpoch = MutableStateFlow(0)`, `val carsonEpochFlow = carsonEpoch.asStateFlow()`, `fun onUserPresent() { carsonEpoch.value++ }`.
  - `MainActivity`: receiver contestuale per `Intent.ACTION_USER_PRESENT` registrato in `onCreate` (handler → `viewModel.onUserPresent()`), deregistrato in `onDestroy`. NB: `ACTION_USER_PRESENT` scatta a ogni sblocco, non a ogni screen-on.

- [ ] **Step 1: ViewModel epoch + receiver in `MainActivity`.**

- [ ] **Step 2: `CarsonText.kt`** — Layout come da interfaccia.

- [ ] **Step 3: `CarsonFavoritesLayout.kt`** — Layout con misura header e piani.

- [ ] **Step 4: `HomeScreen`** — ramo Carson (markers calcolati come oggi, stesso ordine TOP/BOTTOM; hint invariato).

- [ ] **Step 5: `HeaderBlock`** — ramo Carson dentro le due colonne.

- [ ] **Step 6: Verificare la compilazione**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

### Task 9: Verifica finale e validazione manuale

**Files:** nessuno.

- [ ] **Step 1: Tutti i test**

Run: `./gradlew testDebugUnitTest`
Expected: tutti PASS (20 test).

- [ ] **Step 2: Build release-debug**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Installare sul dispositivo (se collegato)**

Run: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
Expected: install riuscita. Se il launcher non è più quello di default dopo l'install: `adb shell cmd package set-home-activity com.riccardopatane.minimallauncher.debug/.MainActivity`.

- [ ] **Step 4: Checklist di validazione manuale per l'utente** (da riportare a fine lavoro):
  1. Impostazioni → look: i 4 livelli cambiano tutto il launcher; DURO = identico a prima.
  2. CRT glow attivo: alone leggero su ogni schermata; griglia sottile/marcata sopra tutto, anche in settings; i tocchi funzionano ovunque.
  3. Griglia marcata + SOFT_3: il testo si scurisce (voluto) ma resta leggibile.
  4. Carson sdc/sdcr: solo pagina principale; header a due colonne con caos dentro; righe a piena larghezza; tap invariati; lista app e settings normali.
  5. 8 preferiti: tutto sempre dentro lo schermo, anche con nomi cortissimi.
  6. Sblocca il telefono: la composizione cambia; apri un'app e torna senza sbloccare: resta identica.
  7. Marker dei preferiti leggibili e non caotici.
- [ ] **Step 5: Nessun commit** — lasciare tutto nel working tree per la validazione dell'utente.
