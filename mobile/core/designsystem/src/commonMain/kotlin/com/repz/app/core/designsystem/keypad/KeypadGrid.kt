package com.repz.app.core.designsystem.keypad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.repz.app.core.designsystem.format.FixedDecimal
import com.repz.app.core.designsystem.format.RepzNumberSymbols

// Digit positions 1-9 (row-major, left to right, top to bottom); RPE mode's 9 choices replace them
// in the same positions (U2 option a).
private val DIGIT_ROWS: List<List<Int>> = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))
private const val RPE_CHOICES_PER_ROW = 3

/** One row of digit keys. */
@Composable
private fun DigitRow(
    digits: List<Int>,
    onDigit: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(KeypadGridGap)) {
        for (digit in digits) {
            DigitKey(digit, onClick = { onDigit(digit) }, modifier = Modifier.weight(1f).height(KeypadKeySize))
        }
    }
}

/** RPE mode's 3x3 grid of [KeypadField.choices] (U2 option a): same positions digits 1-9 use. */
@Composable
private fun RpeChoiceRows(
    field: KeypadField,
    symbols: RepzNumberSymbols,
    onSetRpe: (FixedDecimal) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(KeypadGridGap)) {
        for (row in field.choices.chunked(RPE_CHOICES_PER_ROW)) {
            Row(horizontalArrangement = Arrangement.spacedBy(KeypadGridGap)) {
                for (value in row) {
                    RpeChoiceKey(
                        value = value,
                        symbols = symbols,
                        selected = field.value == value,
                        onClick = { onSetRpe(value) },
                        modifier = Modifier.weight(1f).height(KeypadKeySize),
                    )
                }
            }
        }
    }
}

/**
 * The 4x4 key grid (DESIGN_SYSTEM §9, plan §3.5): a 3-column flexible main area plus an
 * [KeypadNextColumnWidth] rail holding Clear/hide/Next. The grid's total height is identical in
 * every mode (U2 option a "nothing jumps"): weight/reps fill the main area with digits, decimal and
 * backspace and the rail with Clear/hide/Next; RPE fills the main area with its 9 choices plus a
 * full-width hide row and the rail with a taller Clear (rows 1-2) and Next (rows 3-4).
 */
@Composable
internal fun KeypadGrid(
    field: KeypadField,
    symbols: RepzNumberSymbols,
    onDigit: (Int) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onSetRpe: (FixedDecimal) -> Unit,
    decimalSeparatorDescription: String,
    backspaceDescription: String,
    clearLabel: String,
    hideDescription: String,
    onHide: () -> Unit,
    nextLabel: String,
    nextDescription: String,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRpe = field.mode == KeypadMode.Rpe
    val railSpanHeight = KeypadKeySize * 2 + KeypadGridGap

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KeypadGridGap)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(KeypadGridGap)) {
            if (isRpe) {
                RpeChoiceRows(field, symbols, onSetRpe)
                HideKey(
                    contentDescription = hideDescription,
                    onClick = onHide,
                    modifier = Modifier.fillMaxWidth().height(KeypadKeySize),
                )
            } else {
                for (digits in DIGIT_ROWS) {
                    DigitRow(digits, onDigit)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(KeypadGridGap)) {
                    DecimalKey(
                        separator = symbols.decimalSeparator,
                        enabled = field.fractionDigits > 0,
                        contentDescription = decimalSeparatorDescription,
                        onClick = onDecimal,
                        modifier = Modifier.weight(1f).height(KeypadKeySize),
                    )
                    DigitKey(0, onClick = { onDigit(0) }, modifier = Modifier.weight(1f).height(KeypadKeySize))
                    BackspaceKey(
                        contentDescription = backspaceDescription,
                        onClick = onBackspace,
                        modifier = Modifier.weight(1f).height(KeypadKeySize),
                    )
                }
            }
        }
        Column(
            modifier = Modifier.width(KeypadNextColumnWidth),
            verticalArrangement = Arrangement.spacedBy(KeypadGridGap),
        ) {
            if (isRpe) {
                ClearKey(label = clearLabel, onClick = onClear, modifier = Modifier.fillMaxWidth().height(railSpanHeight))
            } else {
                ClearKey(label = clearLabel, onClick = onClear, modifier = Modifier.fillMaxWidth().height(KeypadKeySize))
                HideKey(
                    contentDescription = hideDescription,
                    onClick = onHide,
                    modifier = Modifier.fillMaxWidth().height(KeypadKeySize),
                )
            }
            NextKey(
                label = nextLabel,
                contentDescription = nextDescription,
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(railSpanHeight),
            )
        }
    }
}
