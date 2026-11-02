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
    Extended("extended"),
    Type("type"),
    TypeEmphasized("type-emphasized"),
    Numeric("numeric"),
    Shapes("shapes"),
    SpacingElevation("spacing-elevation"),
    ;

    companion object {
        fun fromKey(key: String?): RepzCatalogPage? = entries.firstOrNull { it.key == key }
    }
}
