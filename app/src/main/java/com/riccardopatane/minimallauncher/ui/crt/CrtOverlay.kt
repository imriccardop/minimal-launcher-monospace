package com.riccardopatane.minimallauncher.ui.crt

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.riccardopatane.minimallauncher.model.CrtGrid

/**
 * Scanline grid (horizontal + vertical) drawn above ALL launcher screens.
 * Draws nothing with NONE and carries no pointer input: the UI below stays
 * fully interactive.
 */
@Composable
fun CrtOverlay(grid: CrtGrid, modifier: Modifier = Modifier) {
    if (grid == CrtGrid.NONE) return
    val density = LocalDensity.current
    val (lineDp, periodDp, alpha) = when (grid) {
        CrtGrid.NONE -> return
        CrtGrid.THIN -> Triple(CrtSpec.THIN_LINE_DP, CrtSpec.THIN_PERIOD_DP, CrtSpec.THIN_ALPHA)
    }
    val linePx = with(density) { lineDp.dp.toPx() }
    val periodPx = with(density) { periodDp.dp.toPx() }

    Canvas(modifier) {
        val color = Color.Black.copy(alpha = alpha)
        var x = 0f
        while (x < size.width) {
            drawRect(color, topLeft = Offset(x, 0f), size = Size(linePx, size.height))
            x += periodPx
        }
        var y = 0f
        while (y < size.height) {
            drawRect(color, topLeft = Offset(0f, y), size = Size(size.width, linePx))
            y += periodPx
        }
    }
}
