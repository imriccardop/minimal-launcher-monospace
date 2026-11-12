package com.riccardopatane.minimallauncher.model

/**
 * Chaotic typography modes for the main page. First entry = default.
 * SDC = "sorry david carson" (case, wrong-letter variants, irregular
 * spacing), SDCR = SDC plus vertically flipped letters.
 */
enum class CarsonMode(val label: String) {
    OFF("off"),
    SDC("sdc"),
    SDCR("sdcr"),
}
