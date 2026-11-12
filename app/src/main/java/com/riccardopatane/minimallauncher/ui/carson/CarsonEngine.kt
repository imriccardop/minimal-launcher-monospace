package com.riccardopatane.minimallauncher.ui.carson

import com.riccardopatane.minimallauncher.model.CarsonMode
import kotlin.random.Random

/** Fixed glyph advance of the pixel monospace font, in em. */
const val MONO_ADVANCE = 0.6f

/**
 * "Wrong" variants per base letter (lowercase key): both cases of the base
 * letter plus its diacritic variants. Letters outside the map have no
 * variants. The sets mirror the approved mockups.
 */
val VARIANT_MAP: Map<Char, List<Char>> = buildMap {
    val sets = mapOf(
        'a' to "AaÁáĂăÂâÄäÀàĀāĄąÅåÃãÆæ",
        'b' to "Bb",
        'c' to "CcÇçĆćĈĉĊċČč",
        'd' to "DdĎďĐđ",
        'e' to "EeÈèÉéÊêËëĒēĖėĘęĚě",
        'f' to "Ff",
        'g' to "GgĜĝĞğĠġĢģ",
        'h' to "HhĤĥĦħ",
        'i' to "IiÌìÍíÎîÏïĨĩĪīİıĮį",
        'j' to "JjĴĵ",
        'k' to "KkĶķ",
        'l' to "LlĹĺĻļĽľŁł",
        'm' to "Mm",
        'n' to "NnÑñŃńŅņŇň",
        'o' to "OoÒòÓóÔôÕõÖöØøŌōŐő",
        'p' to "Pp",
        'q' to "Qq",
        'r' to "RrŔŕŘřŖŗ",
        's' to "SsŚśŜŝŞşŠšȘș",
        't' to "TtŢţŤťŦŧ",
        'u' to "UuÙùÚúÛûÜüŨũŪūŬŭŮůŰűŲų",
        'v' to "Vv",
        'w' to "WwŴŵ",
        'x' to "Xx",
        'y' to "YyÝýŶŷŸÿ",
        'z' to "ZzŹźŻżŽž",
    )
    sets.forEach { (base, chars) -> put(base, chars.toList()) }
}

/** One letter of a Carson row: replaced glyph, size, flip and spacing. */
data class LetterSpec(
    val glyph: String,
    val sizeFactor: Float,
    val flipped: Boolean,
    val spacingDp: Float,
)

/** A full row plan: per-letter specs plus the row's base font size (sp). */
data class RowPlan(
    val letters: List<LetterSpec>,
    val fontSizeSp: Float,
)

/**
 * Deterministic seed for a text: depends only on (unlock epoch, position
 * index, label), so the composition is stable while the phone is watched
 * and re-rolls only on unlock. The epoch is the unlock timestamp: mixing in
 * the wall clock makes consecutive unlocks unrelated to each other.
 */
fun carsonSeed(epoch: Long, index: Int, label: String): Long =
    (epoch * 31 + index) * 31 + label.hashCode()

/**
 * Builds the per-letter specs. Shared by line plans and (in the fill math)
 * by row plans. Randomness comes from [random], so callers control the
 * seed. [spacingMax] = upper bound of the irregular spacing (0 when the
 * caller will replace spacing with fill values).
 *
 * Glyph rule (per letter): ~50% a diacritic variant (uniform over the
 * family minus the base pair), ~50% the plain letter with random case.
 * Letters with no diacritics in the map just flip case 50/50.
 */
internal fun lettersFor(
    text: String,
    mode: CarsonMode,
    random: Random,
    spacingMax: Float,
): List<LetterSpec> = text.map { char ->
    val glyph = if (char.isLetter()) {
        val variants = VARIANT_MAP[char.lowercaseChar()].orEmpty()
        when {
            variants.isEmpty() -> char.toString()
            // metà delle volte: variante con diacritico (casi inclusi)
            variants.size > 2 && random.nextFloat() < 0.5f ->
                variants.drop(2)[random.nextInt(variants.size - 2)].toString()
            // altrimenti: lettera piana, caso casuale
            else -> if (random.nextBoolean()) {
                char.uppercaseChar().toString()
            } else {
                char.lowercaseChar().toString()
            }
        }
    } else {
        char.toString()
    }
    LetterSpec(
        glyph = glyph,
        sizeFactor = 0.85f + random.nextFloat() * 0.30f,
        flipped = mode == CarsonMode.SDCR && random.nextFloat() < 0.25f,
        spacingDp = random.nextFloat() * spacingMax,
    )
}

/**
 * Plan for a header line (clock, date, counters): the base size stays, the
 * chaos is inside the line — per-letter size jitter ±15%, irregular spacing
 * up to 40% of the base size, flips only in SDCR.
 */
fun computeLinePlan(
    text: String,
    mode: CarsonMode,
    seed: Long,
    baseSizeSp: Float,
): RowPlan {
    require(mode != CarsonMode.OFF) { "the engine is not called in OFF mode" }
    return RowPlan(
        letters = lettersFor(text, mode, Random(seed), spacingMax = baseSizeSp * 0.4f),
        fontSizeSp = baseSizeSp,
    )
}

/** Vertical padding of a favorites row: 4dp top + 4dp bottom. */
const val ROW_VERTICAL_PADDING_DP = 8f

/**
 * Line height of Carson rows. 1.0 = the font's natural line height (ascent +
 * descent): as tight as it gets without glyphs touching between rows.
 */
const val ROW_LINE_HEIGHT_FACTOR = 1.0f

/** Marker font size (the marker stays normal in Carson mode). */
const val MARKER_SIZE_SP = 20f

/** Portion of the row width left to the (jittered) letter spacing. */
private const val SPACING_TARGET_FRACTION = 0.08f

/** Marker width in dp, from the fixed-advance font at marker size. */
fun markerWidthDp(marker: String): Float =
    marker.length * MONO_ADVANCE * MARKER_SIZE_SP

private fun glyphSum(letters: List<LetterSpec>, size: Float): Float =
    letters.sumOf { (it.glyph.length * MONO_ADVANCE * size * it.sizeFactor).toDouble() }.toFloat()

/**
 * Sets the per-letter spacing with jitter ±40% around the base and
 * re-normalizes it, so the row spans the width exactly (the residual width
 * is absorbed when [size] is capped below the natural fill size).
 */
private fun fillSpacing(
    letters: List<LetterSpec>,
    size: Float,
    availableWidthDp: Float,
    random: Random,
): List<LetterSpec> {
    val factors = letters.map { 0.6f + random.nextFloat() * 0.8f }
    val base = (availableWidthDp - glyphSum(letters, size)) / factors.sum()
    return letters.zip(factors).map { (spec, factor) -> spec.copy(spacingDp = base * factor) }
}

/**
 * Fill-width row plan: the base size is chosen so glyphs leave the target
 * spacing fraction of the width, then jittered spacing spans the rest. With
 * [maxFontSizeSp], short labels are capped and spacing absorbs the slack.
 */
fun computeRowPlan(
    text: String,
    mode: CarsonMode,
    seed: Long,
    availableWidthDp: Float,
    maxFontSizeSp: Float? = null,
): RowPlan {
    require(mode != CarsonMode.OFF) { "the engine is not called in OFF mode" }
    val random = Random(seed)
    val letters = lettersFor(text, mode, random, spacingMax = 0f)
    val fillSize = letters
        .takeIf { it.isNotEmpty() }
        ?.let { ls ->
            (availableWidthDp * (1 - SPACING_TARGET_FRACTION)) /
                ls.sumOf { (it.glyph.length * MONO_ADVANCE * it.sizeFactor).toDouble() }.toFloat()
        }
        ?: 0f
    val size = if (maxFontSizeSp != null && fillSize > maxFontSizeSp) maxFontSizeSp else fillSize
    return RowPlan(fillSpacing(letters, size, availableWidthDp, random), size)
}

/**
 * "Sta tutto sempre": natural fill size per row; if the sum of natural
 * heights exceeds the budget, every row shrinks by the same scale factor
 * and spacing brings each row back to full width.
 */
fun computeFavoritesPlan(
    labels: List<String>,
    mode: CarsonMode,
    epoch: Long,
    availableWidthDp: Float,
    heightBudgetDp: Float,
): List<RowPlan> {
    require(mode != CarsonMode.OFF) { "the engine is not called in OFF mode" }
    data class Natural(
        val letters: List<LetterSpec>,
        val size: Float,
        val height: Float,
        val random: Random,
    )

    val naturals = labels.mapIndexed { index, label ->
        val random = Random(carsonSeed(epoch, index, label))
        val letters = lettersFor(label, mode, random, spacingMax = 0f)
        // the height model uses the row's MAX size factor: CarsonText gives
        // every letter the same line box, so measured == planned
        val maxFactor = letters.maxOfOrNull { it.sizeFactor } ?: 1f
        val size = letters
            .takeIf { it.isNotEmpty() }
            ?.let { ls ->
                (availableWidthDp * (1 - SPACING_TARGET_FRACTION)) /
                    ls.sumOf { (it.glyph.length * MONO_ADVANCE * it.sizeFactor).toDouble() }.toFloat()
            }
            ?: 0f
        Natural(
            letters,
            size,
            size * ROW_LINE_HEIGHT_FACTOR * maxFactor + ROW_VERTICAL_PADDING_DP,
            random,
        )
    }

    val total = naturals.sumOf { it.height.toDouble() }.toFloat()
    val scale = if (total <= heightBudgetDp) 1f else heightBudgetDp / total
    return naturals.map { natural ->
        val maxFactor = natural.letters.maxOfOrNull { it.sizeFactor } ?: 1f
        val size = if (scale == 1f) {
            natural.size
        } else {
            ((natural.height * scale - ROW_VERTICAL_PADDING_DP) /
                (ROW_LINE_HEIGHT_FACTOR * maxFactor)).coerceAtLeast(0f)
        }
        RowPlan(fillSpacing(natural.letters, size, availableWidthDp, natural.random), size)
    }
}
