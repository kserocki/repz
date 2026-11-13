package com.repz.app.core.designsystem.catalog

/**
 * Fixed specimen strings for the catalog. The catalog is a developer tool, not user-facing, so
 * these are plain constants rather than Compose Resources strings.
 */
internal object CatalogSpecimens {
    const val PL_DIACRITIC_LINE = "Zażółć gęślą jaźń"
    const val TIMER_HERO = "1:09"
    val TIMER_STACK = listOf("1:09", "1:10", "1:11")
    const val WEIGHT_SAMPLE = "82.5 kg"

    // U+00A0 (non-breaking space) as both the thousands separator and the number-unit
    // separator before "kg", per DESIGN_SYSTEM §4.
    const val VOLUME_SAMPLE = "12 340 kg"

    const val HAPTICS_ENABLED_LABEL = "Haptics enabled"
    const val PLAY_ALL_LABEL = "Play all"
    const val LOADER_DESCRIPTION = "Loading"

    // The keypad catalog pages' sample exercise/set context; the live workout feature supplies
    // the real one.
    const val KEYPAD_CONTEXT_LABEL = "Bench Press · Set 3"
}
