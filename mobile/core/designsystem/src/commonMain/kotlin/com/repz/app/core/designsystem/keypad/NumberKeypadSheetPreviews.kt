package com.repz.app.core.designsystem.keypad

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.format.FixedDecimal
import com.repz.app.core.designsystem.theme.RepzTheme

// Sample data only, mirroring plan §3.8's catalog sample (last 82.5 x 8 @ RPE 8, step 2.5,
// context "Bench Press - Set 3") so every preview and the keypad-weight/-reps/-rpe catalog pages
// agree on what a "typical" set looks like.
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
private val WEIGHT_REPS_MODES = listOf(KeypadMode.Weight, KeypadMode.Reps)

internal fun previewWeightRequest(
    value: FixedDecimal? = SAMPLE_WEIGHT,
    last: FixedDecimal? = SAMPLE_WEIGHT,
    modes: List<KeypadMode> = ALL_MODES,
): NumberKeypadRequest =
    NumberKeypadRequest(
        targetKey = "preview-weight",
        field =
            KeypadField(
                mode = KeypadMode.Weight,
                value = value,
                min = FixedDecimal.ZERO,
                max = WEIGHT_MAX,
                step = WEIGHT_STEP,
                fractionDigits = 2,
                sameAsLast = last,
                quickValues = KeypadQuickValues.weightSuggestions(last ?: value, WEIGHT_STEP, FixedDecimal.ZERO, WEIGHT_MAX),
                choices = emptyList(),
            ),
        modes = modes,
        weightUnitLabel = "kg",
        contextLabel = PREVIEW_CONTEXT_LABEL,
        nextAction = KeypadNextAction.Reps,
        stepOptions = WEIGHT_STEP_OPTIONS,
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
 * PL locale symbols (the comma decimal separator, PL mode/chip strings): approximates plan §3.8's
 * "weight editing '82,|'" state. The exact mid-type cursor state ("82," before the fraction digits)
 * lives in the sheet's own `rememberSaveable` buffer (plan T6), which isn't reachable from a
 * `request`-only preview without reopening the frozen §8 signature the workout feature already
 * builds against — flagged to Atlas; Spotter's K2/K3 recordings cover the real mid-type interaction
 * instead.
 */
@Preview(locale = "pl")
@Composable
private fun WeightPlSymbolsLightPreview() {
    KeypadPreview(darkTheme = false, request = previewWeightRequest())
}

@Preview(locale = "pl")
@Composable
private fun WeightPlSymbolsDarkPreview() {
    KeypadPreview(darkTheme = true, request = previewWeightRequest())
}
