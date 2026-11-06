package com.repz.app.core.designsystem.keypad

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.format.FixedDecimal

private val RPE_SELECTED_SAMPLE = FixedDecimal(800)

/** RPE mode, 8.0 selected: tinted `secondaryContainer` among the 9 choices (U2 option a). */
@Preview
@Composable
private fun RpeSelectedLightPreview() {
    KeypadPreview(darkTheme = false, request = previewRpeRequest(value = RPE_SELECTED_SAMPLE))
}

@Preview
@Composable
private fun RpeSelectedDarkPreview() {
    KeypadPreview(darkTheme = true, request = previewRpeRequest(value = RPE_SELECTED_SAMPLE))
}

/** RPE mode, empty (display the em dash, no choice tinted). */
@Preview
@Composable
private fun RpeEmptyLightPreview() {
    KeypadPreview(darkTheme = false, request = previewRpeRequest(value = null))
}

@Preview
@Composable
private fun RpeEmptyDarkPreview() {
    KeypadPreview(darkTheme = true, request = previewRpeRequest(value = null))
}
