package com.repz.app.core.designsystem.keypad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.repz.app.core.designsystem.format.FixedDecimal
import com.repz.app.core.designsystem.format.RepzNumberSymbols

// Digit positions 1-9 (row-major, left to right, top to bottom); RPE mode's 9 choices replace them
// in the same positions (U2 option a).
private val DIGIT_ROWS: List<List<Int>> = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))
private const val RPE_CHOICES_PER_ROW = 3

/** A floor of [KeypadKeySize], stretched to match whichever key its [KeyRow] grows to, e.g. a
 * wrapped "Wyczyść" at 200% PL (plan §3.5 "keys keep 52 dp minimum and grow"). */
private fun Modifier.gridKeyHeight(): Modifier = heightIn(min = KeypadKeySize).fillMaxHeight()

/** A row whose own height is the tallest of its keys' min-intrinsic heights, so a key that needs
 * more room (its rail key included) grows the whole row instead of clipping. */
@Composable
private fun KeyRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(KeypadGridGap),
        content = content,
    )
}

@Composable
private fun RowScope.RpeChoiceCells(
    row: List<FixedDecimal>,
    field: KeypadField,
    symbols: RepzNumberSymbols,
    onSetRpe: (FixedDecimal) -> Unit,
) {
    for (value in row) {
        RpeChoiceKey(
            value = value,
            symbols = symbols,
            selected = field.value == value,
            onClick = { onSetRpe(value) },
            modifier = Modifier.weight(1f).gridKeyHeight(),
        )
    }
}

/**
 * Two rows next to a spanning rail key (Next or RPE's Clear), sized by [IntrinsicSize.Min] and
 * split evenly between the two rows via `weight(1f)`, so the rail key beside them always matches
 * their combined height (ADR-0028).
 */
@Composable
private fun RowScope.TwoRowKeyColumn(
    topRow: @Composable RowScope.() -> Unit,
    bottomRow: @Composable RowScope.() -> Unit,
) {
    Column(modifier = Modifier.weight(1f).height(IntrinsicSize.Min), verticalArrangement = Arrangement.spacedBy(KeypadGridGap)) {
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(KeypadGridGap), content = topRow)
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(KeypadGridGap), content = bottomRow)
    }
}

/**
 * Weight/reps rows (DESIGN_SYSTEM §9, plan §3.5; row-major "1 2 3 backspace / 4 5 6 Clear / 7 8 9
 * Next / . 0 keyboard_hide"): rows 1-2 each share one [KeyRow] height with their rail key
 * (Backspace, then Clear); row 3-4 sit in a two-row column next to Next, inside one more [KeyRow]
 * so Next's height always matches that column's.
 */
@Composable
private fun DigitKeyRows(
    field: KeypadField,
    symbols: RepzNumberSymbols,
    onDigit: (Int) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
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
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(KeypadGridGap)) {
        KeyRow {
            for (digit in DIGIT_ROWS[0]) {
                DigitKey(digit, onClick = { onDigit(digit) }, modifier = Modifier.weight(1f).gridKeyHeight())
            }
            BackspaceKey(
                contentDescription = backspaceDescription,
                onClick = onBackspace,
                modifier = Modifier.width(KeypadNextColumnWidth).gridKeyHeight(),
            )
        }
        KeyRow {
            for (digit in DIGIT_ROWS[1]) {
                DigitKey(digit, onClick = { onDigit(digit) }, modifier = Modifier.weight(1f).gridKeyHeight())
            }
            ClearKey(label = clearLabel, onClick = onClear, modifier = Modifier.width(KeypadNextColumnWidth).gridKeyHeight())
        }
        KeyRow {
            TwoRowKeyColumn(
                topRow = {
                    for (digit in DIGIT_ROWS[2]) {
                        DigitKey(digit, onClick = { onDigit(digit) }, modifier = Modifier.weight(1f).gridKeyHeight())
                    }
                },
                bottomRow = {
                    DecimalKey(
                        separator = symbols.decimalSeparator,
                        enabled = field.fractionDigits > 0,
                        contentDescription = decimalSeparatorDescription,
                        onClick = onDecimal,
                        modifier = Modifier.weight(1f).gridKeyHeight(),
                    )
                    DigitKey(0, onClick = { onDigit(0) }, modifier = Modifier.weight(1f).gridKeyHeight())
                    HideKey(contentDescription = hideDescription, onClick = onHide, modifier = Modifier.weight(1f).gridKeyHeight())
                },
            )
            NextKey(
                label = nextLabel,
                contentDescription = nextDescription,
                onClick = onNext,
                modifier = Modifier.width(KeypadNextColumnWidth).fillMaxHeight(),
            )
        }
    }
}

/**
 * RPE mode's rows (U2 option a): the 9 [KeypadField.choices] replace digits 1-9 in the same 3x3
 * positions; row 4 becomes a full-width hide key. The rail is a taller Clear (rows 1-2, no
 * Backspace in RPE mode) and Next (row 3 + the hide row), each matching its own two-row column's
 * height the same way [DigitKeyRows] does.
 */
@Composable
private fun RpeKeyRows(
    field: KeypadField,
    symbols: RepzNumberSymbols,
    onSetRpe: (FixedDecimal) -> Unit,
    clearLabel: String,
    onClear: () -> Unit,
    hideDescription: String,
    onHide: () -> Unit,
    nextLabel: String,
    nextDescription: String,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val choiceRows = field.choices.chunked(RPE_CHOICES_PER_ROW)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(KeypadGridGap)) {
        KeyRow {
            TwoRowKeyColumn(
                topRow = { RpeChoiceCells(choiceRows.getOrElse(0) { emptyList() }, field, symbols, onSetRpe) },
                bottomRow = { RpeChoiceCells(choiceRows.getOrElse(1) { emptyList() }, field, symbols, onSetRpe) },
            )
            ClearKey(label = clearLabel, onClick = onClear, modifier = Modifier.width(KeypadNextColumnWidth).fillMaxHeight())
        }
        KeyRow {
            TwoRowKeyColumn(
                topRow = { RpeChoiceCells(choiceRows.getOrElse(2) { emptyList() }, field, symbols, onSetRpe) },
                bottomRow = {
                    HideKey(contentDescription = hideDescription, onClick = onHide, modifier = Modifier.fillMaxWidth().gridKeyHeight())
                },
            )
            NextKey(
                label = nextLabel,
                contentDescription = nextDescription,
                onClick = onNext,
                modifier = Modifier.width(KeypadNextColumnWidth).fillMaxHeight(),
            )
        }
    }
}

/**
 * The 4x4 key grid (DESIGN_SYSTEM §9, plan §3.5; exact placement per boards 05 §7 and 09 state 11):
 * [DigitKeyRows] for Weight/Reps, [RpeKeyRows] for RPE (U2 option a). The grid's total height is
 * identical in every mode (U2 option a "nothing jumps"). Digits/separator/backspace are ignored in
 * RPE mode (reachable only via a hardware keyboard).
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
    if (field.mode == KeypadMode.Rpe) {
        RpeKeyRows(
            field = field,
            symbols = symbols,
            onSetRpe = onSetRpe,
            clearLabel = clearLabel,
            onClear = onClear,
            hideDescription = hideDescription,
            onHide = onHide,
            nextLabel = nextLabel,
            nextDescription = nextDescription,
            onNext = onNext,
            modifier = modifier,
        )
    } else {
        DigitKeyRows(
            field = field,
            symbols = symbols,
            onDigit = onDigit,
            onDecimal = onDecimal,
            onBackspace = onBackspace,
            onClear = onClear,
            decimalSeparatorDescription = decimalSeparatorDescription,
            backspaceDescription = backspaceDescription,
            clearLabel = clearLabel,
            hideDescription = hideDescription,
            onHide = onHide,
            nextLabel = nextLabel,
            nextDescription = nextDescription,
            onNext = onNext,
            modifier = modifier,
        )
    }
}
