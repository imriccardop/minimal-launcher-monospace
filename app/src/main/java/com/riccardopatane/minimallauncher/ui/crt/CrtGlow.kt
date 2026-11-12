package com.riccardopatane.minimallauncher.ui.crt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Whether the CRT glow is active; provided once in MainActivity. */
val LocalCrtGlow: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { false }

/**
 * Light glow shadow (the only approved intensity) in the text's own color,
 * or null when the effect is off.
 */
@Composable
fun crtGlowShadow(textColor: Color): Shadow? =
    if (LocalCrtGlow.current) {
        Shadow(
            color = textColor,
            blurRadius = with(LocalDensity.current) { CrtSpec.GLOW_BLUR_DP.dp.toPx() },
        )
    } else {
        null
    }
