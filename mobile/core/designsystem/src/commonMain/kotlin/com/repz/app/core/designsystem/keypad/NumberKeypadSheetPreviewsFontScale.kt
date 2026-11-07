package com.repz.app.core.designsystem.keypad

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.format.FixedDecimal

private val TWO_MODES = listOf(KeypadMode.Weight, KeypadMode.Reps)

/** Only Weight/Reps modes (RPE disabled for this field, e.g. an `isRpeEnabled = false` request). */
@Preview
@Composable
private fun TwoModesLightPreview() {
    KeypadPreview(darkTheme = false, request = previewWeightRequest(modes = TWO_MODES))
}

@Preview
@Composable
private fun TwoModesDarkPreview() {
    KeypadPreview(darkTheme = true, request = previewWeightRequest(modes = TWO_MODES))
}

/** 200% font: the Next label wraps to 2 lines, chips wrap, keys keep their minimum size. */
@Preview(fontScale = 2f)
@Composable
private fun FontScale200LightPreview() {
    KeypadPreview(darkTheme = false, request = previewWeightRequest())
}

@Preview(fontScale = 2f)
@Composable
private fun FontScale200DarkPreview() {
    KeypadPreview(darkTheme = true, request = previewWeightRequest())
}

// review-1 F2/Q2: the longest possible value ("2204.62" lb) on a 360 dp-wide phone, at 200% and
// 150% font — the hero must still render on one line (auto-shrinking down to 22 sp) and the row's
// height must stay fixed, so the ±/chips/grid below it never move.
private val HERO_MAX_LB = FixedDecimal(220_462)
private val HERO_MAX_LB_STEP = FixedDecimal(500)

private fun heroOneLineRequest(): NumberKeypadRequest =
    previewWeightRequest(
        value = HERO_MAX_LB,
        last = null,
        max = HERO_MAX_LB,
        step = HERO_MAX_LB_STEP,
        weightUnitLabel = "lb",
        stepOptions = emptyList(),
    )

@Preview(widthDp = 360, fontScale = 2f)
@Composable
private fun HeroOneLineFontScale200LightPreview() {
    KeypadPreview(darkTheme = false, request = heroOneLineRequest())
}

@Preview(widthDp = 360, fontScale = 2f)
@Composable
private fun HeroOneLineFontScale200DarkPreview() {
    KeypadPreview(darkTheme = true, request = heroOneLineRequest())
}

@Preview(widthDp = 360, fontScale = 1.5f)
@Composable
private fun HeroOneLineFontScale150LightPreview() {
    KeypadPreview(darkTheme = false, request = heroOneLineRequest())
}

@Preview(widthDp = 360, fontScale = 1.5f)
@Composable
private fun HeroOneLineFontScale150DarkPreview() {
    KeypadPreview(darkTheme = true, request = heroOneLineRequest())
}
