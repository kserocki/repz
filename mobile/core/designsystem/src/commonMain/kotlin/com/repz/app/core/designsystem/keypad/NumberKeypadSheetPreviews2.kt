package com.repz.app.core.designsystem.keypad

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/** Weight mode with no "Same as last" chip (the caller passed no last value, plan §3.4 T9). */
@Preview
@Composable
private fun WeightNoHistoryLightPreview() {
    KeypadPreview(darkTheme = false, request = previewWeightRequest(last = null))
}

@Preview
@Composable
private fun WeightNoHistoryDarkPreview() {
    KeypadPreview(darkTheme = true, request = previewWeightRequest(last = null))
}

/** Reps mode: the decimal key disabled, chips 5/6/8/10/12 plus "Same as last". */
@Preview
@Composable
private fun RepsLightPreview() {
    KeypadPreview(darkTheme = false, request = previewRepsRequest())
}

@Preview
@Composable
private fun RepsDarkPreview() {
    KeypadPreview(darkTheme = true, request = previewRepsRequest())
}
