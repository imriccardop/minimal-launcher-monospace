package com.riccardopatane.minimallauncher.ui.carson

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riccardopatane.minimallauncher.ui.theme.launcherStyle

/**
 * One Carson row/line: places each letter with its size factor, flip and
 * spacing from the plan. No rotation — SDCR flips are vertical only.
 * Letters are bottom-aligned so their baselines line up despite the size
 * jitter.
 */
@Composable
fun CarsonText(plan: RowPlan, color: Color, modifier: Modifier = Modifier) {
    val density = LocalDensity.current.density
    // every letter shares ONE line box (the max of the row), so the measured
    // row height matches the engine's height model exactly
    val maxFactor = plan.letters.maxOfOrNull { it.sizeFactor } ?: 1f
    Layout(
        modifier = modifier,
        content = {
            plan.letters.forEach { letter ->
                BasicText(
                    text = letter.glyph,
                    style = launcherStyle(
                        fontSize = (plan.fontSizeSp * letter.sizeFactor).sp,
                        color = color,
                        lineHeightFactor = (maxFactor * ROW_LINE_HEIGHT_FACTOR) / letter.sizeFactor,
                    ),
                    modifier = Modifier.graphicsLayer {
                        if (letter.flipped) scaleY = -1f
                    },
                )
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(Constraints()) }
        val rowHeight = placeables.maxOfOrNull { it.height } ?: 0
        val rowWidth = placeables.zip(plan.letters).sumOf { (placeable, letter) ->
            (placeable.width + letter.spacingDp * density).toDouble()
        }.toInt()
        layout(rowWidth, rowHeight) {
            var x = 0
            placeables.zip(plan.letters).forEach { (placeable, letter) ->
                placeable.placeRelative(x, rowHeight - placeable.height)
                x += placeable.width + (letter.spacingDp * density).toInt()
            }
        }
    }
}
