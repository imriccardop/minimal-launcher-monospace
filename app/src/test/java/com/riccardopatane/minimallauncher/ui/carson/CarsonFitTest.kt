package com.riccardopatane.minimallauncher.ui.carson

import com.riccardopatane.minimallauncher.model.CarsonMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CarsonFitTest {

    @Test
    fun row_fills_the_available_width() {
        val plan = computeRowPlan("WhatsApp", CarsonMode.SDC, 5L, availableWidthDp = 300f)
        val width = plan.letters.sumOf { it.spacingDp.toDouble() } +
            plan.letters.sumOf { (MONO_ADVANCE * plan.fontSizeSp * it.sizeFactor).toDouble() }
        assertEquals(300f, width.toFloat(), 0.5f)
    }

    @Test
    fun max_size_caps_and_spacing_absorbs() {
        val plan = computeRowPlan("a", CarsonMode.SDC, 5L, availableWidthDp = 300f, maxFontSizeSp = 80f)
        assertEquals(80f, plan.fontSizeSp, 0.001f)
        val width = plan.letters.sumOf { it.spacingDp.toDouble() } +
            plan.letters.sumOf { (MONO_ADVANCE * plan.fontSizeSp * it.sizeFactor).toDouble() }
        assertEquals(300f, width.toFloat(), 0.5f)
    }

    @Test
    fun marker_width_is_excluded_by_the_caller_but_predictable() {
        assertEquals(0f, markerWidthDp(""))
        assertEquals("⟶".length * MONO_ADVANCE * 20f, markerWidthDp("⟶"))
    }

    @Test
    fun eight_short_labels_always_fit_the_budget() {
        val labels = listOf("a", "b", "c", "d", "e", "f", "g", "h")
        val plans = computeFavoritesPlan(
            labels, CarsonMode.SDCR, epoch = 3, availableWidthDp = 340f, heightBudgetDp = 400f,
        )
        assertEquals(8, plans.size)
        val total = plans.sumOf { (it.fontSizeSp * ROW_LINE_HEIGHT_FACTOR + ROW_VERTICAL_PADDING_DP).toDouble() }
        assertTrue(total <= 400f + 0.5f)
        plans.forEach { assertTrue(it.fontSizeSp > 0f) }
    }

    @Test
    fun overflow_scales_all_rows_proportionally() {
        val plans = computeFavoritesPlan(
            listOf("WhatsApp", "Telegram", "Gmail"),
            CarsonMode.SDC,
            epoch = 1,
            availableWidthDp = 340f,
            heightBudgetDp = 60f,
        )
        val total = plans.sumOf { (it.fontSizeSp * ROW_LINE_HEIGHT_FACTOR + ROW_VERTICAL_PADDING_DP).toDouble() }
        assertTrue(total <= 60f + 0.5f)
        // altezze naturali pre-scala (dimensione di riempimento × 1.25 + 16)
        val naturals = listOf("WhatsApp", "Telegram", "Gmail").map { 340f / (it.length * MONO_ADVANCE) }
        val natHeights = naturals.map { it * ROW_LINE_HEIGHT_FACTOR + ROW_VERTICAL_PADDING_DP }
        // ogni riga è stata ridotta con lo STESSO fattore di scala
        val ratioA = (plans[0].fontSizeSp * ROW_LINE_HEIGHT_FACTOR + ROW_VERTICAL_PADDING_DP) / natHeights[0]
        val ratioB = (plans[1].fontSizeSp * ROW_LINE_HEIGHT_FACTOR + ROW_VERTICAL_PADDING_DP) / natHeights[1]
        val ratioC = (plans[2].fontSizeSp * ROW_LINE_HEIGHT_FACTOR + ROW_VERTICAL_PADDING_DP) / natHeights[2]
        assertEquals(ratioA, ratioB, 0.01f)
        assertEquals(ratioA, ratioC, 0.01f)
    }

    @Test
    fun same_epoch_same_plans_across_calls() {
        val a = computeFavoritesPlan(listOf("Maps", "Note"), CarsonMode.SDC, epoch = 9, 340f, 400f)
        val b = computeFavoritesPlan(listOf("Maps", "Note"), CarsonMode.SDC, epoch = 9, 340f, 400f)
        assertEquals(a, b)
    }

    @Test
    fun row_height_model_uses_the_max_size_factor() {
        // il modello altezze deve includere il sizeFactor massimo della riga,
        // altrimenti il misurato supera il pianificato e l'ultima riga esce
        // dallo schermo (review finding 1)
        val plans = computeFavoritesPlan(
            listOf("WhatsApp", "Telegram", "Gmail", "Maps", "Spotify", "Note", "Uber", "X"),
            CarsonMode.SDCR,
            epoch = 5,
            availableWidthDp = 340f,
            heightBudgetDp = 400f,
        )
        val total = plans.sumOf { p ->
            val maxFactor = p.letters.maxOfOrNull { it.sizeFactor } ?: 1f
            (p.fontSizeSp * ROW_LINE_HEIGHT_FACTOR * maxFactor + ROW_VERTICAL_PADDING_DP).toDouble()
        }
        assertTrue(total <= 400f + 0.5f)
        plans.forEach { assertTrue(it.fontSizeSp >= 0f) }
    }

    @Test
    fun empty_label_does_not_crash() {
        val plan = computeRowPlan("", CarsonMode.SDC, 1L, 300f)
        assertEquals(emptyList<LetterSpec>(), plan.letters)
        val plans = computeFavoritesPlan(listOf(""), CarsonMode.SDC, epoch = 1, 340f, 400f)
        assertEquals(1, plans.size)
        assertTrue(plans[0].fontSizeSp >= 0f)
    }

    @Test
    fun tiny_budget_never_goes_negative() {
        val plans = computeFavoritesPlan(listOf("a", "b", "c", "d"), CarsonMode.SDC, epoch = 2, 340f, 10f)
        plans.forEach { assertTrue(it.fontSizeSp >= 0f) }
    }
}
