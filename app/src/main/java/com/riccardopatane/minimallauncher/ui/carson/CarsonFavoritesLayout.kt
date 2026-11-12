package com.riccardopatane.minimallauncher.ui.carson

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riccardopatane.minimallauncher.model.CarsonMode
import com.riccardopatane.minimallauncher.model.FavoritePosition
import com.riccardopatane.minimallauncher.ui.theme.LocalLauncherColors
import com.riccardopatane.minimallauncher.ui.theme.launcherStyle

/**
 * Carson home: measures the header first, then fills the remaining height
 * with the favorites rows ("sta tutto sempre" — the engine shrinks rows
 * proportionally when the natural fill sizes overflow the budget). The
 * marker stays normal, the name fills the remaining width. Interactions
 * unchanged: each row launches its app, TOP/BOTTOM order as today.
 */
@Composable
fun CarsonFavoritesLayout(
    header: @Composable () -> Unit,
    labels: List<String>,
    markers: List<String>,
    mode: CarsonMode,
    epoch: Long,
    position: FavoritePosition,
    onLaunch: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLauncherColors.current
    val density = LocalDensity.current.density
    val horizontalPaddingPx = with(LocalDensity.current) { 20.dp.toPx() }
    // self-correcting fudge: measured rows are a few dp taller than the model
    // (sp/dp and fontScale). Each pass measures, and if the rows overshoot
    // the budget the next pass shrinks it by exactly the overshoot.
    var heightFudgeDp by remember { mutableStateOf(0f) }

    SubcomposeLayout(modifier = modifier) { constraints ->
        val headerPlaceable = subcompose("header") { header() }[0]
            .measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))

        val budgetDp = (constraints.maxHeight - headerPlaceable.height) / density
        val contentWidthDp = (constraints.maxWidth - horizontalPaddingPx * 2) / density
        val markerWidth = markers.maxOfOrNull { markerWidthDp("$it ") } ?: 0f
        val plans = computeFavoritesPlan(
            labels = labels,
            mode = mode,
            epoch = epoch,
            availableWidthDp = contentWidthDp - markerWidth,
            heightBudgetDp = (budgetDp - heightFudgeDp).coerceAtLeast(0f),
        )

        val rowPlaceables = subcompose("rows") {
            labels.forEachIndexed { index, label ->
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLaunch(index) }
                        .padding(
                            horizontal = 20.dp,
                            vertical = (ROW_VERTICAL_PADDING_DP / 2).dp,
                        ),
                ) {
                    BasicText(
                        text = "${markers.getOrElse(index) { "" }} ",
                        style = launcherStyle(20.sp, colors.text),
                    )
                    CarsonText(plan = plans[index], color = colors.text)
                }
            }
        }.map {
            // minHeight = 0: fillMaxSize impone minHeight = maxHeight; senza
            // questo azzeramento ogni riga misurerebbe l'intera altezza schermo
            it.measure(
                constraints.copy(
                    minWidth = 0,
                    minHeight = 0,
                    maxWidth = constraints.maxWidth,
                ),
            )
        }

        val totalRowsHeight = rowPlaceables.sumOf { it.height }
        // monotonic convergence: if the rows overshoot, raise the fudge for
        // the next measure pass (bounded by the budget itself)
        val overshoot = (totalRowsHeight.toFloat() - budgetDp * density).coerceAtLeast(0f) / density
        if (overshoot - heightFudgeDp > 0.5f && heightFudgeDp < budgetDp) {
            heightFudgeDp = overshoot
        }
        val rowsStartY = when (position) {
            FavoritePosition.TOP -> headerPlaceable.height
            FavoritePosition.BOTTOM -> (constraints.maxHeight - totalRowsHeight)
                .coerceAtLeast(headerPlaceable.height)
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            headerPlaceable.placeRelative(0, 0)
            var y = rowsStartY
            rowPlaceables.forEach { placeable ->
                placeable.placeRelative(0, y)
                y += placeable.height
            }
        }
    }
}
