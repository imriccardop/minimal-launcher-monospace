package com.riccardopatane.minimallauncher.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.riccardopatane.minimallauncher.R

val Black = Color(0xFF000000)
val White = Color(0xFFFFFFFF)
val Gray = Color(0xFF666666)

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
): TextStyle = TextStyle(
    fontFamily = UndefinedMedium,
    fontSize = fontSize,
    lineHeight = fontSize * lineHeightFactor,
    color = color,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)
