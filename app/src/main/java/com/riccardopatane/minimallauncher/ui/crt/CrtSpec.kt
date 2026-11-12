package com.riccardopatane.minimallauncher.ui.crt

/**
 * Approved CRT effect values, in dp: mockup pixels are read as dp on the
 * device so the effect keeps the proportions seen in the browser.
 */
object CrtSpec {
    /** Blur radius of the light glow ("leggera" intensity only). */
    const val GLOW_BLUR_DP = 4f

    /** Thin grid: 1dp lines every 3dp, 18% black. */
    const val THIN_LINE_DP = 1f
    const val THIN_PERIOD_DP = 3f
    const val THIN_ALPHA = 0.18f
}
