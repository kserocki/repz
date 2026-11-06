package com.repz.app.core.designsystem.keypad

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

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
