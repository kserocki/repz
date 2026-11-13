package com.repz.app.core.designsystem.component

import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.repz.app.core.designsystem.haptics.RepzHaptics
import kotlin.math.roundToInt

/**
 * A continuous or stepped value picker (DESIGN_SYSTEM §9, board 04 §21 "Default rest"): M3's own
 * Expressive [Slider] defaults already match the board exactly (16 dp track, 4×44 dp handle,
 * `primary` active / `secondaryContainer` inactive). [valueText] (e.g. "2:30") becomes the slider's
 * accessible state description, since the raw float [value] alone isn't announced meaningfully.
 * Each drag step that crosses a [steps] boundary fires [RepzHaptics.tick] once (MOTION.md §3 "slider
 * detent"); a continuous slider ([steps] = 0) never ticks.
 */
@Composable
fun RepzSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueText: String,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val haptics = RepzHaptics.current

    Slider(
        value = value,
        onValueChange = { newValue ->
            val crossedAStep =
                steps > 0 && sliderStepIndex(newValue, valueRange, steps) != sliderStepIndex(value, valueRange, steps)
            if (crossedAStep) haptics.tick()
            onValueChange(newValue)
        },
        modifier = modifier.semantics { stateDescription = valueText },
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
    )
}

/**
 * The 0-based position [value] is nearest to, among the `steps + 2` evenly spaced points a stepped
 * slider snaps to (the two ends plus [steps] in between, matching M3's own `Slider` `steps`
 * semantics). A continuous slider ([steps] `<= 0`, or a zero-width [valueRange]) has no such
 * positions, so this always returns 0 and [RepzSlider] never ticks.
 */
internal fun sliderStepIndex(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
): Int {
    val span = valueRange.endInclusive - valueRange.start
    return if (steps <= 0 || span <= 0f) {
        0
    } else {
        val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
        (fraction * (steps + 1)).roundToInt()
    }
}
