package com.riccardopatane.minimallauncher.model

/**
 * Intensity levels of the monochrome look. The background stays near-black in
 * every level: the softening happens on the text. First entry = default.
 */
enum class ThemeLevel(val label: String) {
    DURO("hard"),
    SOFT_1("soft 1"),
    SOFT_2("soft 2"),
    SOFT_3("soft 3"),
}
