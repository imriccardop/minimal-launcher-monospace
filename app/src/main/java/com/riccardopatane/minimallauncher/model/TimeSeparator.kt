package com.riccardopatane.minimallauncher.model

/** Separator between hours and minutes in the home clock. */
enum class TimeSeparator(val label: String, val patternChar: String) {
    COLON(":", ":"),
    UNDERSCORE("_", "_"),
    DOT(".", "."),
    SPACE("space", " "),
    NONE("none", ""),
}
