package com.riccardopatane.minimallauncher.ui.carson

import com.riccardopatane.minimallauncher.model.CarsonMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CarsonEngineTest {

    @Test
    fun same_seed_same_plan() {
        val a = computeLinePlan("WhatsApp", CarsonMode.SDCR, 42L, 20f)
        val b = computeLinePlan("WhatsApp", CarsonMode.SDCR, 42L, 20f)
        assertEquals(a, b)
    }

    @Test
    fun different_seeds_differ() {
        val a = computeLinePlan("WhatsApp", CarsonMode.SDC, 1L, 20f)
        val b = computeLinePlan("WhatsApp", CarsonMode.SDC, 2L, 20f)
        assertNotEquals(a.letters.map { it.glyph }, b.letters.map { it.glyph })
    }

    @Test
    fun flips_only_in_sdcr() {
        val plain = computeLinePlan("WhatsApp", CarsonMode.SDC, 7L, 20f)
        val rot = computeLinePlan("WhatsApp", CarsonMode.SDCR, 7L, 20f)
        assertTrue(plain.letters.none { it.flipped })
        // con 8 lettere e p=0.25, almeno un capovolgimento è pressoché certo; il
        // test fissa un seme noto e verifica che il capovolgimento avvenga
        assertTrue(rot.letters.any { it.flipped })
    }

    @Test
    fun letters_get_variants_but_digits_and_symbols_do_not() {
        val plan = computeLinePlan("23_45%", CarsonMode.SDCR, 9L, 18f)
        assertEquals(listOf("2", "3", "_", "4", "5", "%"), plan.letters.map { it.glyph })
    }

    @Test
    fun letter_variants_come_from_the_map() {
        val plan = computeLinePlan("Aa", CarsonMode.SDC, 3L, 20f)
        // il "set di provenienza" è la lettera ORIGINALE, non il glifo: il
        // lowercase di un diacritico non si decompone (à resta à)
        plan.letters.forEachIndexed { index, spec ->
            val base = "Aa"[index].lowercaseChar()
            assertTrue(VARIANT_MAP.getValue(base).contains(spec.glyph[0]))
        }
    }

    @Test
    fun size_factor_and_spacing_stay_in_range() {
        val plan = computeLinePlan("Impostazioni", CarsonMode.SDC, 11L, 56f)
        plan.letters.forEach {
            assertTrue(it.sizeFactor in 0.85f..1.15f)
            assertTrue(it.spacingDp in 0f..(56f * 0.4f))
        }
    }

    @Test
    fun variant_map_contains_only_font_supported_glyphs() {
        // Ĕ ĕ Ĭ ĭ Ŀ ŀ Ŏ ŏ mancano dal font pixel (undefined medium): se scelti,
        // Android cadrebbe su Roboto rompendo l'avanzamento monospace.
        val removed = listOf('Ĕ', 'ĕ', 'Ĭ', 'ĭ', 'Ŀ', 'ŀ', 'Ŏ', 'ŏ')
        removed.forEach { glyph ->
            VARIANT_MAP.values.forEach { assertTrue(glyph !in it) }
        }
        assertEquals("EeÈèÉéÊêËëĒēĖėĘęĚě".toList(), VARIANT_MAP.getValue('e'))
        assertEquals("IiÌìÍíÎîÏïĨĩĪīİıĮį".toList(), VARIANT_MAP.getValue('i'))
        assertEquals("LlĹĺĻļĽľŁł".toList(), VARIANT_MAP.getValue('l'))
        assertEquals("OoÒòÓóÔôÕõÖöØøŌōŐő".toList(), VARIANT_MAP.getValue('o'))
    }

    @Test
    fun diacritic_rate_is_about_half() {
        // ~50% di varianti con diacritico: con 400 lettere il conteggio deve
        // stare ben lontano sia dall'89% (uniforme) che dallo 0%
        val plan = computeLinePlan("e".repeat(400), CarsonMode.SDC, 17L, 20f)
        val weird = plan.letters.count { it.glyph != "e" && it.glyph != "E" }
        assertTrue("tasso diacritici = $weird/400", weird in 170..230)
    }
}
