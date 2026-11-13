package com.repz.app.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/** The ambient [Density] with [maxFontScale] as a ceiling: for a numeric value whose layout (an
 * auto-sized hero display, a set row's weight/reps field) was designed up to that scale, so a
 * higher system font scale only shrinks the auto-sized text further instead of overflowing its
 * fixed-width container. Every other text on screen keeps following the real system font scale. */
@Composable
internal fun rememberCappedFontScaleDensity(maxFontScale: Float): Density {
    val ambientDensity = LocalDensity.current
    return remember(ambientDensity, maxFontScale) {
        Density(density = ambientDensity.density, fontScale = ambientDensity.fontScale.coerceAtMost(maxFontScale))
    }
}
