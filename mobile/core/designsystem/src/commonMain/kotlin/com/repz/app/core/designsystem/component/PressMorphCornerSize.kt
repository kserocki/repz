package com.repz.app.core.designsystem.component

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

/**
 * Lerps between a pill's rest radius — half of the shape's own measured [restDimensionPx], not a
 * precomputed size-token value, so the result is a true pill at any actual height (a wrapped label,
 * a larger font scale) and not only at a component's minimum — and [pressedRadiusPx], as
 * [pressFraction] runs 0 (rest) to 1 (pressed). A plain function so it is unit-testable without a
 * [Density]/[Size]; [PressMorphCornerSize] is the [CornerSize] wrapper every morphing pill shape
 * (`RepzButton`, `RepzHeroStartButton`, `RepzConnectedButtonGroup`, `RepzSplitButton`) builds on.
 */
internal fun pressMorphCornerRadiusPx(
    restDimensionPx: Float,
    pressedRadiusPx: Float,
    pressFraction: Float,
): Float {
    val restRadiusPx = restDimensionPx / 2f
    val clampedFraction = pressFraction.coerceIn(0f, 1f)
    return restRadiusPx + (pressedRadiusPx - restRadiusPx) * clampedFraction
}

/**
 * A [CornerSize] that reads the shape's real measured size at draw time (Coach review-1, required
 * change 4): the rest radius is half of the *actual* `shapeSize.minDimension`, not a precomputed
 * `height / 2` token, so a button/hero/connected-group/split-button segment stays a true pill even
 * when its label wraps or the font scale grows it past its minimum height.
 */
internal class PressMorphCornerSize(
    private val pressedRadius: Dp,
    private val pressFraction: Float,
) : CornerSize {
    override fun toPx(
        shapeSize: Size,
        density: Density,
    ): Float {
        val pressedRadiusPx = with(density) { pressedRadius.toPx() }
        return pressMorphCornerRadiusPx(shapeSize.minDimension, pressedRadiusPx, pressFraction)
    }
}
