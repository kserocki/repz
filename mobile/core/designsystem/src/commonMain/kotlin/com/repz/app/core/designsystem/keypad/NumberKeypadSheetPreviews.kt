package com.repz.app.core.designsystem.keypad

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.format.FixedDecimal
import com.repz.app.core.designsystem.theme.RepzTheme

// Sample data only, mirroring the catalog sample (last 82.5 x 8 @ RPE 8, step 2.5, context
// "Bench Press - Set 3") so every preview and the keypad-weight/-reps/-rpe catalog pages agree on
// what a "typical" set looks like.
private val SAMPLE_WEIGHT = FixedDecimal(8250)
private val WEIGHT_MAX = FixedDecimal.ofWhole(1000)
private val WEIGHT_STEP = FixedDecimal(250)
private val WEIGHT_STEP_OPTIONS = listOf(50, 125, 250, 500).map { FixedDecimal(it.toLong()) }
private val SAMPLE_REPS = FixedDecimal.ofWhole(8)
private val REPS_MAX = FixedDecimal.ofWhole(999)
private val REPS_STEP = FixedDecimal.ofWhole(1)
private val RPE_CHOICES: List<FixedDecimal> = listOf(600, 650, 700, 750, 800, 850, 900, 950, 1000).map { FixedDecimal(it.toLong()) }
private val RPE_STEP = FixedDecimal(50)
private val SAMPLE_RPE = FixedDecimal(800)

// Mirrors CatalogSpecimens.KEYPAD_CONTEXT_LABEL in the catalog package; kept local here (not
// imported) so this production preview file doesn't depend on the debug-only catalog package.
private const val PREVIEW_CONTEXT_LABEL = "Bench Press · Set 3"
private val ALL_MODES = listOf(KeypadMode.Weight, KeypadMode.Reps, KeypadMode.Rpe)

internal fun previewWeightRequest(
    value: FixedDecimal? = SAMPLE_WEIGHT,
    last: FixedDecimal? = SAMPLE_WEIGHT,
    modes: List<KeypadMode> = ALL_MODES,
    max: FixedDecimal = WEIGHT_MAX,
    step: FixedDecimal = WEIGHT_STEP,
    weightUnitLabel: String = "kg",
    stepOptions: List<FixedDecimal> = WEIGHT_STEP_OPTIONS,
): NumberKeypadRequest =
    NumberKeypadRequest(
        targetKey = "preview-weight",
        field =
            KeypadField(
                mode = KeypadMode.Weight,
                value = value,
                min = FixedDecimal.ZERO,
                max = max,
                step = step,
                fractionDigits = 2,
                sameAsLast = last,
                quickValues = KeypadQuickValues.weightSuggestions(last ?: value, step, FixedDecimal.ZERO, max),
                choices = emptyList(),
            ),
        modes = modes,
        weightUnitLabel = weightUnitLabel,
        contextLabel = PREVIEW_CONTEXT_LABEL,
        nextAction = KeypadNextAction.Reps,
        stepOptions = stepOptions,
    )

internal fun previewRepsRequest(
    value: FixedDecimal? = SAMPLE_REPS,
    last: FixedDecimal? = SAMPLE_REPS,
): NumberKeypadRequest =
    NumberKeypadRequest(
        targetKey = "preview-reps",
        field =
            KeypadField(
                mode = KeypadMode.Reps,
                value = value,
                min = FixedDecimal.ZERO,
                max = REPS_MAX,
                step = REPS_STEP,
                fractionDigits = 0,
                sameAsLast = last,
                quickValues = KeypadQuickValues.repsSuggestions,
                choices = emptyList(),
            ),
        modes = ALL_MODES,
        weightUnitLabel = "kg",
        contextLabel = PREVIEW_CONTEXT_LABEL,
        nextAction = KeypadNextAction.Rpe,
        stepOptions = emptyList(),
    )

internal fun previewRpeRequest(value: FixedDecimal? = SAMPLE_RPE): NumberKeypadRequest =
    NumberKeypadRequest(
        targetKey = "preview-rpe",
        field =
            KeypadField(
                mode = KeypadMode.Rpe,
                value = value,
                min = RPE_CHOICES.first(),
                max = RPE_CHOICES.last(),
                step = RPE_STEP,
                fractionDigits = 1,
                sameAsLast = SAMPLE_RPE,
                quickValues = emptyList(),
                choices = RPE_CHOICES,
            ),
        modes = ALL_MODES,
        weightUnitLabel = "kg",
        contextLabel = PREVIEW_CONTEXT_LABEL,
        nextAction = KeypadNextAction.NextSet,
        stepOptions = emptyList(),
    )

/** Renders [request] in a plain [RepzTheme], the way every other `core:designsystem` preview does. */
@Composable
internal fun KeypadPreview(
    darkTheme: Boolean,
    request: NumberKeypadRequest?,
    reducedMotion: Boolean? = null,
) {
    RepzTheme(darkTheme = darkTheme, reducedMotion = reducedMotion) {
        NumberKeypadSheet(
            request = request,
            onValueChange = {},
            onModeSelect = {},
            onStepSelect = {},
            onNext = {},
            onDismiss = {},
        )
    }
}

/**
 * Renders [NumberKeypadSheetStateless] directly with a forced [buffer]/[showLimitError], so a
 * preview can show a frame [NumberKeypadSheet] itself would only reach through real typing (a
 * mid-type buffer, or the context line just after a refusal) without touching
 * [NumberKeypadSheet]'s own public signature.
 */
@Composable
internal fun KeypadPreviewStateless(
    darkTheme: Boolean,
    request: NumberKeypadRequest,
    buffer: KeypadBuffer,
    showLimitError: Boolean = false,
) {
    RepzTheme(darkTheme = darkTheme) {
        NumberKeypadSheetStateless(
            request = request,
            buffer = buffer,
            showLimitError = showLimitError,
            onInput = { true },
            onModeSelect = {},
            onStepSelect = {},
            onNext = {},
            onDismiss = {},
        )
    }
}

/** Weight mode, untouched, with Same as last + quick-value chips + the Step chip all visible. */
@Preview
@Composable
private fun WeightWithChipsLightPreview() {
    KeypadPreview(darkTheme = false, request = previewWeightRequest())
}

@Preview
@Composable
private fun WeightWithChipsDarkPreview() {
    KeypadPreview(darkTheme = true, request = previewWeightRequest())
}

/**
 * Weight mode mid-type: the buffer reads "82." (a touched, not-yet-complete fraction), shown as
 * "82,|" once the PL locale substitutes the decimal separator. Forced directly through
 * [KeypadPreviewStateless] since this exact buffer only otherwise arises from real typing.
 */
@Preview(locale = "pl")
@Composable
private fun WeightPlMidTypeLightPreview() {
    KeypadPreviewStateless(
        darkTheme = false,
        request = previewWeightRequest(),
        buffer = KeypadBuffer(text = "82.", isUntouched = false),
    )
}

@Preview(locale = "pl")
@Composable
private fun WeightPlMidTypeDarkPreview() {
    KeypadPreviewStateless(
        darkTheme = true,
        request = previewWeightRequest(),
        buffer = KeypadBuffer(text = "82.", isUntouched = false),
    )
}

/** Over-max: the context line swaps to the "Weight must be 0-1000 kg" limit message in `error`
 * after a refused keystroke; the buffer stays at the last accepted value, 1000. */
@Preview
@Composable
private fun WeightOverMaxErrorLightPreview() {
    KeypadPreviewStateless(
        darkTheme = false,
        request = previewWeightRequest(value = WEIGHT_MAX, last = null),
        buffer = KeypadRules.bufferFor(WEIGHT_MAX),
        showLimitError = true,
    )
}

@Preview
@Composable
private fun WeightOverMaxErrorDarkPreview() {
    KeypadPreviewStateless(
        darkTheme = true,
        request = previewWeightRequest(value = WEIGHT_MAX, last = null),
        buffer = KeypadRules.bufferFor(WEIGHT_MAX),
        showLimitError = true,
    )
}
