package com.repz.app.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/** The ambient [Density] with [maxFontScale] as a ceiling, so a numeric value laid out up to that
 * scale (an auto-sized hero display, a set row's weight/reps field) only shrinks further above it
 * instead of overflowing its fixed-width container. Other text keeps the real system font scale. */
@Composable
internal fun rememberCappedFontScaleDensity(maxFontScale: Float): Density {
    val ambientDensity = LocalDensity.current
    return remember(ambientDensity, maxFontScale) {
        Density(density = ambientDensity.density, fontScale = ambientDensity.fontScale.coerceAtMost(maxFontScale))
    }
}
