package com.repz.app.core.designsystem.keypad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.repz.app.core.designsystem.format.FixedDecimal.Companion.HUNDREDTHS_PER_UNIT
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
import com.repz.app.core.designsystem.theme.rememberCappedFontScaleDensity
import org.jetbrains.compose.resources.stringResource

// The hero stays on one line from "" to "2204.62", at up to 200% font, on a 360 dp-wide screen, so
// the ±/chips/grid never move while typing. `numericHero`'s own wdth 112 and tabular figures are
// kept (DESIGN_SYSTEM §13); only size/line-height/tracking are resized to 52/56, shrinking in 1 sp
// steps down to 22 sp so the longest value always fits one line.
private val HeroMaxFontSize = 52.sp
private val HeroMinFontSize = 22.sp
private val HeroStepSize = 1.sp
private val HeroLineHeight = 56.sp
private val HeroLetterSpacing = (-2f / 52f).em

// Caps the hero's own font scale at 2.0x (iOS AX5 is roughly 3.1x), so it never grows past what the
// 52/56 sizing was designed for, while the mode labels and every other text in the sheet keep
// following the real system font scale (DESIGN_SYSTEM §13, ADR-0028).
private const val HERO_MAX_FONT_SCALE = 2f

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

/** The limit message shown after a refusal; `null` for RPE, which has no limit message defined —
 * a refused RPE ± still fires Reject without replacing the context line. */
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
 * (MOTION.md §1). The context line swaps to the limit message in `error` after a refusal. */
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
            val heroStyle =
                RepzTheme.typography.numericHero.copy(
                    fontSize = HeroMaxFontSize,
                    lineHeight = HeroLineHeight,
                    letterSpacing = HeroLetterSpacing,
                    lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.None),
                )
            CompositionLocalProvider(LocalDensity provides rememberCappedFontScaleDensity(HERO_MAX_FONT_SCALE)) {
                Text(
                    text = strings.displayText,
                    style = heroStyle,
                    autoSize =
                        TextAutoSize.StepBased(minFontSize = HeroMinFontSize, maxFontSize = HeroMaxFontSize, stepSize = HeroStepSize),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
                            contentDescription = strings.valueDescription
                            customActions =
                                listOf(
                                    CustomAccessibilityAction(strings.increaseDescription) { onIncrease() },
                                    CustomAccessibilityAction(strings.decreaseDescription) { onDecrease() },
                                )
                        },
                )
            }
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
