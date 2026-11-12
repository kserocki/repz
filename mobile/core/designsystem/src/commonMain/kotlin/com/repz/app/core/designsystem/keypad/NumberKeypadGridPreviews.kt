package com.repz.app.core.designsystem.keypad

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.format.RepzNumberSymbols
import com.repz.app.core.designsystem.theme.RepzTheme

/**
 * Isolates [KeypadGrid] from the rest of the sheet, so a layout regression that collapses a row to
 * 0×0 (the decimal/0/hide row, or RPE's middle choice row and hide key) is immediately,
 * unambiguously visible here. Every key position — all 12 main-area keys plus Backspace/Clear/Next
 * across both rail spans in Weight/Reps mode, or the 9 RPE choices plus hide/Clear/Next in RPE
 * mode — must render with a visible, non-zero, tappable size.
 */
@Composable
private fun KeypadGridPreview(
    darkTheme: Boolean,
    field: KeypadField,
) {
    RepzTheme(darkTheme = darkTheme) {
        Surface {
            KeypadGrid(
                field = field,
                symbols = RepzNumberSymbols.Default,
                onDigit = {},
                onDecimal = {},
                onBackspace = {},
                onClear = {},
                onSetRpe = {},
                decimalSeparatorDescription = "Decimal separator",
                backspaceDescription = "Delete digit",
                clearLabel = "Clear",
                hideDescription = "Hide keypad",
                onHide = {},
                nextLabel = "Reps",
                nextDescription = "Next: repetitions",
                onNext = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview
@Composable
private fun KeypadGridWeightLightPreview() {
    KeypadGridPreview(darkTheme = false, field = previewWeightRequest().field)
}

@Preview
@Composable
private fun KeypadGridWeightDarkPreview() {
    KeypadGridPreview(darkTheme = true, field = previewWeightRequest().field)
}

@Preview
@Composable
private fun KeypadGridRpeLightPreview() {
    KeypadGridPreview(darkTheme = false, field = previewRpeRequest().field)
}

@Preview
@Composable
private fun KeypadGridRpeDarkPreview() {
    KeypadGridPreview(darkTheme = true, field = previewRpeRequest().field)
}

@Preview(fontScale = 2f, locale = "pl")
@Composable
private fun KeypadGridWeightFontScale200PlPreview() {
    KeypadGridPreview(darkTheme = false, field = previewWeightRequest().field)
}
