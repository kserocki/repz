package com.repz.app.core.designsystem.keypad

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/** PL strings on RPE mode, pairing `keypad-weight`/`keypad-rpe` for the PL/font-scale checks. */
@Preview(locale = "pl")
@Composable
private fun RpePlLightPreview() {
    KeypadPreview(darkTheme = false, request = previewRpeRequest())
}

@Preview(locale = "pl")
@Composable
private fun RpePlDarkPreview() {
    KeypadPreview(darkTheme = true, request = previewRpeRequest())
}

/** Reduced motion (MOT-10): the sheet appears without a slide; key presses snap/crossfade instead
 * of morphing (not visible in a static screenshot, but exercises the reduced-motion code path). */
@Preview
@Composable
private fun ReducedMotionPreview() {
    KeypadPreview(darkTheme = false, request = previewWeightRequest(), reducedMotion = true)
}

/** At 200% PL font the Reps segment's full label ("Powtórzenia") no longer fits the segment on one
 * line, so it falls back to the short label ("Powt."); the accessible name stays the full word. */
@Preview(locale = "pl", fontScale = 2f)
@Composable
private fun ModeShortLabelLightPreview() {
    KeypadPreview(darkTheme = false, request = previewWeightRequest())
}

@Preview(locale = "pl", fontScale = 2f)
@Composable
private fun ModeShortLabelDarkPreview() {
    KeypadPreview(darkTheme = true, request = previewWeightRequest())
}
