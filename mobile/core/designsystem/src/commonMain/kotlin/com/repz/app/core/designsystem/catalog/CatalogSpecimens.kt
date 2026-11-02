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

    // U+00A0 (non-breaking space) as the thousands separator, per DESIGN_SYSTEM §4's grouping rule.
    const val VOLUME_SAMPLE = "12 340 kg"
}
