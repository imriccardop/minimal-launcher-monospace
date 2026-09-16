package com.riccardopatane.minimallauncher.model

/**
 * Date formats selectable in settings. Declaration order is the list order;
 * the first is the default; the extended one is last by user choice.
 */
enum class DateFormat(val pattern: String) {
    ABBREVIATED("EEE d MMM"),        // Sun 13 Sep
    WITH_YEAR("d MMM yy"),           // 13 Sep 26
    NUMERIC("EEE d/MM"),             // Sun 13/09
    EXTENDED("EEEE d MMMM"),         // Sunday 13 September
}
