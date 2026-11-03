package com.repz.app.core.designsystem.catalog

/**
 * The debug-only design-system catalog's pages. This is a developer tool, not user-facing: the
 * [key] is a stable identifier used by the platform launch hooks (an Android intent extra, an iOS
 * launch argument), never shown to end users and never localized.
 */
enum class RepzCatalogPage(
    val key: String,
) {
    Colors("colors"),
    Extended1("extended-1"),
    Extended2("extended-2"),
    Extended3("extended-3"),
    Type("type"),
    TypeEmphasized1("type-emphasized-1"),
    TypeEmphasized2("type-emphasized-2"),
    Numeric1("numeric-1"),
    Numeric2("numeric-2"),
    Shapes("shapes"),
    SpacingElevation1("spacing-elevation-1"),
    SpacingElevation2("spacing-elevation-2"),
    MotionSprings1("motion-springs-1"),
    MotionSprings2("motion-springs-2"),
    MotionTweens("motion-tweens"),
    MotionMorphs("motion-morphs"),
    Haptics("haptics"),
    ;

    companion object {
        fun fromKey(key: String?): RepzCatalogPage? = entries.firstOrNull { it.key == key }
    }
}
