package com.repz.app.core.designsystem.catalog

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/** The 36 M3 colour-scheme roles (DESIGN_SYSTEM §3.1) of the active scheme: swatch, role name and
 * hex. Reads the live `MaterialTheme.colorScheme`, so it also reflects dynamic colour. */
@Composable
internal fun ColorsCatalogPage() {
    val scheme = MaterialTheme.colorScheme
    val entries =
        listOf(
            ColorEntry("primary", scheme.primary),
            ColorEntry("onPrimary", scheme.onPrimary),
            ColorEntry("primaryContainer", scheme.primaryContainer),
            ColorEntry("onPrimaryContainer", scheme.onPrimaryContainer),
            ColorEntry("inversePrimary", scheme.inversePrimary),
            ColorEntry("secondary", scheme.secondary),
            ColorEntry("onSecondary", scheme.onSecondary),
            ColorEntry("secondaryContainer", scheme.secondaryContainer),
            ColorEntry("onSecondaryContainer", scheme.onSecondaryContainer),
            ColorEntry("tertiary", scheme.tertiary),
            ColorEntry("onTertiary", scheme.onTertiary),
            ColorEntry("tertiaryContainer", scheme.tertiaryContainer),
            ColorEntry("onTertiaryContainer", scheme.onTertiaryContainer),
            ColorEntry("error", scheme.error),
            ColorEntry("onError", scheme.onError),
            ColorEntry("errorContainer", scheme.errorContainer),
            ColorEntry("onErrorContainer", scheme.onErrorContainer),
            ColorEntry("background", scheme.background),
            ColorEntry("onBackground", scheme.onBackground),
            ColorEntry("surface", scheme.surface),
            ColorEntry("onSurface", scheme.onSurface),
            ColorEntry("surfaceVariant", scheme.surfaceVariant),
            ColorEntry("onSurfaceVariant", scheme.onSurfaceVariant),
            ColorEntry("surfaceTint", scheme.surfaceTint),
            ColorEntry("inverseSurface", scheme.inverseSurface),
            ColorEntry("inverseOnSurface", scheme.inverseOnSurface),
            ColorEntry("outline", scheme.outline),
            ColorEntry("outlineVariant", scheme.outlineVariant),
            ColorEntry("scrim", scheme.scrim),
            ColorEntry("surfaceBright", scheme.surfaceBright),
            ColorEntry("surfaceDim", scheme.surfaceDim),
            ColorEntry("surfaceContainerLowest", scheme.surfaceContainerLowest),
            ColorEntry("surfaceContainerLow", scheme.surfaceContainerLow),
            ColorEntry("surfaceContainer", scheme.surfaceContainer),
            ColorEntry("surfaceContainerHigh", scheme.surfaceContainerHigh),
            ColorEntry("surfaceContainerHighest", scheme.surfaceContainerHighest),
        )
    ColorSwatchGrid(entries = entries, columns = 3)
}
