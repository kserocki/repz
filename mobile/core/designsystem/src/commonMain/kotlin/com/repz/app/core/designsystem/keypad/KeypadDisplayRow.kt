package com.repz.app.core.designsystem.keypad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import com.repz.app.core.designsystem.format.RepzNumberSymbols
import com.repz.app.core.designsystem.generated.resources.Res
import com.repz.app.core.designsystem.generated.resources.keypad_context_line
import com.repz.app.core.designsystem.generated.resources.keypad_decrease_description
import com.repz.app.core.designsystem.generated.resources.keypad_decrease_label
import com.repz.app.core.designsystem.generated.resources.keypad_empty_value
import com.repz.app.core.designsystem.generated.resources.keypad_increase_description
import com.repz.app.core.designsystem.generated.resources.keypad_increase_label
import com.repz.app.core.designsystem.generated.resources.keypad_mode_reps
import com.repz.app.core.designsystem.generated.resources.keypad_mode_rpe
import com.repz.app.core.designsystem.generated.resources.keypad_reps_limit
import com.repz.app.core.designsystem.generated.resources.keypad_value_description
import com.repz.app.core.designsystem.generated.resources.keypad_weight_limit
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme
import org.jetbrains.compose.resources.stringResource

private const val HUNDREDTHS_PER_UNIT = 100L

/** Every string [KeypadDisplayRow] shows or announces, computed once per recomposition so the row
 * composable itself stays a plain layout assembly. */
private data class KeypadDisplayStrings(
    val displayText: String,
    val valueDescription: String,
    val contextLine: String,
    val increaseLabel: String,
    val decreaseLabel: String,
    val increaseDescription: String,
    val decreaseDescription: String,
)

private fun substituteDecimalSeparator(
    text: String,
    separator: Char,
): String = text.map { if (it == '.') separator else it }.joinToString("")

@Composable
private fun contextLineModeLabel(
    field: KeypadField,
    weightUnitLabel: String,
): String =
    when (field.mode) {
        KeypadMode.Weight -> weightUnitLabel
        KeypadMode.Reps -> stringResource(Res.string.keypad_mode_reps)
        KeypadMode.Rpe -> stringResource(Res.string.keypad_mode_rpe)
    }

/** The limit message after a refusal (U5); `null` for RPE, which has none defined (plan §2.1 lists
 * weight/reps only) — a refused RPE ± still fires Reject without replacing the context line. */
@Composable
private fun limitText(
    field: KeypadField,
    weightUnitLabel: String,
    symbols: RepzNumberSymbols,
): String? =
    when (field.mode) {
        KeypadMode.Weight -> {
            stringResource(Res.string.keypad_weight_limit, field.min.format(symbols), field.max.format(symbols), weightUnitLabel)
        }

        KeypadMode.Reps -> {
            stringResource(
                Res.string.keypad_reps_limit,
                (field.min.hundredths / HUNDREDTHS_PER_UNIT).toInt(),
                (field.max.hundredths / HUNDREDTHS_PER_UNIT).toInt(),
            )
        }

        KeypadMode.Rpe -> {
            null
        }
    }

@Composable
private fun rememberKeypadDisplayStrings(
    request: NumberKeypadRequest,
    buffer: KeypadBuffer,
    showLimitError: Boolean,
    symbols: RepzNumberSymbols,
): KeypadDisplayStrings {
    val field = request.field
    val emptyValueText = stringResource(Res.string.keypad_empty_value)
    val displayText =
        if (buffer.text.isEmpty()) emptyValueText else substituteDecimalSeparator(buffer.text, symbols.decimalSeparator)

    val modeLabel = contextLineModeLabel(field, request.weightUnitLabel)
    val normalContextLine = stringResource(Res.string.keypad_context_line, modeLabel, request.contextLabel)
    val refusalText = limitText(field, request.weightUnitLabel, symbols)
    val contextLine = if (showLimitError && refusalText != null) refusalText else normalContextLine

    val parsedValue = KeypadRules.valueOf(buffer)
    val valueDescription =
        when {
            parsedValue == null -> {
                emptyValueText
            }

            field.mode == KeypadMode.Weight -> {
                stringResource(Res.string.keypad_value_description, parsedValue.format(symbols), request.weightUnitLabel)
            }

            else -> {
                parsedValue.format(symbols)
            }
        }
    val stepWithUnit =
        if (field.mode == KeypadMode.Weight) {
            stringResource(Res.string.keypad_value_description, field.step.format(symbols), request.weightUnitLabel)
        } else {
            field.step.format(symbols)
        }

    return KeypadDisplayStrings(
        displayText = displayText,
        valueDescription = valueDescription,
        contextLine = contextLine,
        increaseLabel = stringResource(Res.string.keypad_increase_label, field.step.format(symbols)),
        decreaseLabel = stringResource(Res.string.keypad_decrease_label, field.step.format(symbols)),
        increaseDescription = stringResource(Res.string.keypad_increase_description, stepWithUnit),
        decreaseDescription = stringResource(Res.string.keypad_decrease_description, stepWithUnit),
    )
}

/** ± (60 dp) either side of the numeric display (`numericHero` 52 sp); the display never animates
 * (MOTION.md §1). The context line swaps to the limit message in `error` after a refusal (U5). */
@Composable
internal fun KeypadDisplayRow(
    request: NumberKeypadRequest,
    buffer: KeypadBuffer,
    showLimitError: Boolean,
    symbols: RepzNumberSymbols,
    onIncrease: () -> Boolean,
    onDecrease: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    val strings = rememberKeypadDisplayStrings(request, buffer, showLimitError, symbols)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm),
    ) {
        PlusMinusButton(
            label = strings.decreaseLabel,
            contentDescription = strings.decreaseDescription,
            enabled = true,
            onStep = onDecrease,
        )
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = strings.displayText,
                style = RepzTheme.typography.numericHero,
                modifier =
                    Modifier.semantics(mergeDescendants = true) {
                        contentDescription = strings.valueDescription
                        customActions =
                            listOf(
                                CustomAccessibilityAction(strings.increaseDescription) { onIncrease() },
                                CustomAccessibilityAction(strings.decreaseDescription) { onDecrease() },
                            )
                    },
            )
            Text(
                text = strings.contextLine,
                style = MaterialTheme.typography.bodySmall,
                color = if (showLimitError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PlusMinusButton(
            label = strings.increaseLabel,
            contentDescription = strings.increaseDescription,
            enabled = true,
            onStep = onIncrease,
        )
    }
}
