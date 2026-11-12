package com.riccardopatane.minimallauncher.ui.theme

import androidx.compose.ui.graphics.Color
import com.riccardopatane.minimallauncher.model.ThemeLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class PaletteTest {

    @Test
    fun duro_has_today_palette() {
        val p = paletteFor(ThemeLevel.DURO)
        assertEquals(Color(0xFF000000), p.background)
        assertEquals(Color(0xFFFFFFFF), p.text)
        assertEquals(Color(0xFF666666), p.secondary) // #666 di oggi, invariato
    }

    @Test
    fun soft_levels_have_approved_hexes() {
        assertEquals(
            Color(0xFF0A0A0A) to Color(0xFFC2C2C2),
            paletteFor(ThemeLevel.SOFT_1).let { it.background to it.text },
        )
        assertEquals(
            Color(0xFF080808) to Color(0xFFA5A5A5),
            paletteFor(ThemeLevel.SOFT_2).let { it.background to it.text },
        )
        assertEquals(
            Color(0xFF060606) to Color(0xFF8A8A8A),
            paletteFor(ThemeLevel.SOFT_3).let { it.background to it.text },
        )
    }

    @Test
    fun soft_secondary_is_60_percent_toward_text() {
        assertEquals(Color(0xFF787878), paletteFor(ThemeLevel.SOFT_1).secondary)
        assertEquals(Color(0xFF666666), paletteFor(ThemeLevel.SOFT_2).secondary)
        assertEquals(Color(0xFF555555), paletteFor(ThemeLevel.SOFT_3).secondary)
    }

    @Test
    fun blendARGB_blends_each_channel() {
        assertEquals(0xFF787878L, blendARGB(0xFF0A0A0A, 0xFFC2C2C2, 0.6f))
        assertEquals(0xFF000000L, blendARGB(0xFF000000, 0xFFFFFFFF, 0f))
        assertEquals(0xFFFFFFFFL, blendARGB(0xFF000000, 0xFFFFFFFF, 1f))
    }
}
