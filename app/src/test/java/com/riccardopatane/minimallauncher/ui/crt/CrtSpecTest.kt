package com.riccardopatane.minimallauncher.ui.crt

import com.riccardopatane.minimallauncher.model.CrtGrid
import org.junit.Assert.assertEquals
import org.junit.Test

class CrtSpecTest {

    @Test
    fun glow_blur_is_4dp() {
        assertEquals(4f, CrtSpec.GLOW_BLUR_DP)
    }

    @Test
    fun thin_grid_is_1px_every_3px_at_18_percent() {
        assertEquals(1f, CrtSpec.THIN_LINE_DP)
        assertEquals(3f, CrtSpec.THIN_PERIOD_DP)
        assertEquals(0.18f, CrtSpec.THIN_ALPHA)
    }

    @Test
    fun none_is_first_entry_and_default() {
        assertEquals(CrtGrid.NONE, CrtGrid.entries.first())
    }
}
