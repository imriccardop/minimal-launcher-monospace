package com.riccardopatane.minimallauncher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.riccardopatane.minimallauncher.R
import com.riccardopatane.minimallauncher.model.ThemeLevel
import com.riccardopatane.minimallauncher.ui.crt.crtGlowShadow

val Black = Color(0xFF000000)
val White = Color(0xFFFFFFFF)
val Gray = Color(0xFF666666)

/** Text/background/secondary colors of one look level. */
data class LauncherPalette(
    val text: Color,
    val background: Color,
    val secondary: Color,
)

/** Channel-wise blend in sRGB space (like ArgbEvaluator). */
fun blendARGB(from: Long, to: Long, fraction: Float): Long {
    fun channel(shift: Int): Long {
        val a = (from shr shift) and 0xFF
        val b = (to shr shift) and 0xFF
        return (a + ((b - a) * fraction.toDouble()).toInt()) and 0xFF
    }
    return (channel(24) shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
}

/**
 * Palette per look level. DURO reproduces today's colors exactly (secondary
 * #666); the soft levels derive their secondary as the 60%-toward-text blend
 * of background and text.
 */
fun paletteFor(level: ThemeLevel): LauncherPalette {
    val (text, background) = when (level) {
        ThemeLevel.DURO -> White to Black
        ThemeLevel.SOFT_1 -> Color(0xFFC2C2C2) to Color(0xFF0A0A0A)
        ThemeLevel.SOFT_2 -> Color(0xFFA5A5A5) to Color(0xFF080808)
        ThemeLevel.SOFT_3 -> Color(0xFF8A8A8A) to Color(0xFF060606)
    }
    val secondary = when (level) {
        ThemeLevel.DURO -> Gray
        else -> Color(blendARGB(background.toArgb().toLong(), text.toArgb().toLong(), 0.6f).toInt())
    }
    return LauncherPalette(text, background, secondary)
}

val LocalLauncherColors: ProvidableCompositionLocal<LauncherPalette> =
    staticCompositionLocalOf { paletteFor(ThemeLevel.DURO) }

/**
 * Theme-aware mono style: same as monoStyle but with the palette color made
 * explicit at the call site, plus the CRT glow shadow when the effect is on.
 */
@Composable
fun launcherStyle(
    fontSize: TextUnit,
    color: Color,
    lineHeightFactor: Float = 1.25f,
): TextStyle = monoStyle(fontSize, color, lineHeightFactor, shadow = crtGlowShadow(color))

val UndefinedMedium = FontFamily(
    Font(R.font.undefined_medium, FontWeight.Normal),
)

/**
 * Default mono style: pixel font with no font padding (line-height 1.25 to
 * avoid glyph clipping), never synthetic bold. lineHeightFactor tunes the
 * line spacing: lower = tighter lines.
 */
@OptIn(ExperimentalTextApi::class)
fun monoStyle(
    fontSize: TextUnit,
    color: Color = White,
    lineHeightFactor: Float = 1.25f,
    shadow: Shadow? = null,
): TextStyle = TextStyle(
    fontFamily = UndefinedMedium,
    fontSize = fontSize,
    lineHeight = fontSize * lineHeightFactor,
    color = color,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    shadow = shadow,
)
