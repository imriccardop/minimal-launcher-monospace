package com.riccardopatane.minimallauncher.model

private val SUPERSCRIPT_DIGITS = listOf("⁰", "¹", "²", "³", "⁴", "⁵", "⁶", "⁷", "⁸", "⁹")

/**
 * Optional prefix before the favorite apps. Declaration order is the settings
 * list order; the default is NONE (no prefix).
 */
enum class FavoriteMarker(val label: String) {
    SUPERSCRIPT("⁴ ³ ² ¹ ⁰"),
    ASTERISM("⁂"),
    ARROW("⟶"),
    DIAGONAL("↘"),
    GEQ("≥"),
    TILDE("〜"),
    NONE("none"),
}

/** Superscript number: 12 → "¹²" */
fun toSuperscript(n: Int): String =
    n.toString().map { SUPERSCRIPT_DIGITS[it - '0'] }.joinToString("")
