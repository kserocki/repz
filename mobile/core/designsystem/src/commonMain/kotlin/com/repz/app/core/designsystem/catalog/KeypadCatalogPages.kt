package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.repz.app.core.designsystem.format.FixedDecimal
import com.repz.app.core.designsystem.format.rememberRepzNumberSymbols
import com.repz.app.core.designsystem.haptics.LocalRepzHaptics
import com.repz.app.core.designsystem.haptics.RepzHaptics
import com.repz.app.core.designsystem.haptics.rememberPlatformHapticPerformer
import com.repz.app.core.designsystem.keypad.KeypadField
import com.repz.app.core.designsystem.keypad.KeypadMode
import com.repz.app.core.designsystem.keypad.KeypadNextAction
import com.repz.app.core.designsystem.keypad.KeypadQuickValues
import com.repz.app.core.designsystem.keypad.NumberKeypadRequest
import com.repz.app.core.designsystem.keypad.NumberKeypadSheet
import com.repz.app.core.designsystem.theme.RepzSpacing

// Hundredths for the Kg/Lb sample fields below (Detekt's MagicNumber doesn't exempt enum
// constructor arguments the way it exempts a plain property declaration).
private const val KG_MAX_HUNDREDTHS = 1000_00L
private const val KG_STEP_HALF_HUNDREDTHS = 50L
private const val KG_STEP_QUARTER_HUNDREDTHS = 125L
private const val KG_STEP_DEFAULT_HUNDREDTHS = 250L
private const val KG_STEP_DOUBLE_HUNDREDTHS = 500L
private const val KG_SAMPLE_HUNDREDTHS = 8250L
private const val LB_MAX_HUNDREDTHS = 220462L
private const val LB_STEP_SMALL_HUNDREDTHS = 100L
private const val LB_STEP_DEFAULT_HUNDREDTHS = 250L
private const val LB_STEP_DOUBLE_HUNDREDTHS = 500L
private const val LB_SAMPLE_HUNDREDTHS = 18500L

// Sample data only: the catalog plays the caller role with FixedDecimal directly (it can't see
// core:ui's domain-aware mapping, which SetValueKeypadMappingTest covers instead).
private enum class CatalogWeightUnit(
    val label: String,
    val max: FixedDecimal,
    val stepOptions: List<FixedDecimal>,
    val sample: FixedDecimal,
) {
    Kg(
        label = "kg",
        max = FixedDecimal(KG_MAX_HUNDREDTHS),
        stepOptions =
            listOf(KG_STEP_HALF_HUNDREDTHS, KG_STEP_QUARTER_HUNDREDTHS, KG_STEP_DEFAULT_HUNDREDTHS, KG_STEP_DOUBLE_HUNDREDTHS)
                .map { FixedDecimal(it) },
        sample = FixedDecimal(KG_SAMPLE_HUNDREDTHS),
    ),
    Lb(
        label = "lb",
        max = FixedDecimal(LB_MAX_HUNDREDTHS),
        stepOptions =
            listOf(LB_STEP_SMALL_HUNDREDTHS, LB_STEP_DEFAULT_HUNDREDTHS, LB_STEP_DOUBLE_HUNDREDTHS).map { FixedDecimal(it) },
        sample = FixedDecimal(LB_SAMPLE_HUNDREDTHS),
    ),
}

private const val DEFAULT_WEIGHT_STEP_INDEX = 2 // 2.5 kg / 5 lb (both units' 3rd option)
private val SAMPLE_REPS = FixedDecimal.ofWhole(8)
private val REPS_MAX = FixedDecimal.ofWhole(999)
private val REPS_STEP = FixedDecimal.ofWhole(1)
private const val RPE_MIN_HUNDREDTHS = 600L
private const val RPE_MAX_HUNDREDTHS = 1000L
private const val RPE_STEP_HUNDREDTHS = 50L
private val RPE_CHOICES: List<FixedDecimal> =
    generateSequence(RPE_MIN_HUNDREDTHS) { it + RPE_STEP_HUNDREDTHS }
        .takeWhile { it <= RPE_MAX_HUNDREDTHS }
        .map { FixedDecimal(it) }
        .toList()
private val RPE_STEP = FixedDecimal(RPE_STEP_HUNDREDTHS)
private val SAMPLE_RPE = FixedDecimal(800)

private fun catalogWeightField(
    unit: CatalogWeightUnit,
    step: FixedDecimal,
    value: FixedDecimal?,
    last: FixedDecimal?,
): KeypadField {
    val base = last ?: value
    return KeypadField(
        mode = KeypadMode.Weight,
        value = value,
        min = FixedDecimal.ZERO,
        max = unit.max,
        step = step,
        fractionDigits = 2,
        sameAsLast = last,
        quickValues = KeypadQuickValues.weightSuggestions(base = base, step = step, min = FixedDecimal.ZERO, max = unit.max),
        choices = emptyList(),
    )
}

private fun catalogRepsField(
    value: FixedDecimal?,
    last: FixedDecimal?,
): KeypadField =
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
    )

private fun catalogRpeField(
    value: FixedDecimal?,
    last: FixedDecimal?,
): KeypadField =
    KeypadField(
        mode = KeypadMode.Rpe,
        value = value,
        min = RPE_CHOICES.first(),
        max = RPE_CHOICES.last(),
        step = RPE_STEP,
        fractionDigits = 1,
        sameAsLast = last,
        quickValues = emptyList(),
        choices = RPE_CHOICES,
    )

/** The last 3 lines a catalog page shows: the last emitted value, the last mode switch, and the
 * last Next/Dismiss/Step event (plan §3.8). */
internal class CatalogEventLog {
    var lastValue by mutableStateOf("value: (none yet)")
    var lastMode by mutableStateOf("mode: (unchanged)")
    var lastAction by mutableStateOf("action: (none yet)")

    val lines: List<String> get() = listOf(lastValue, lastMode, lastAction)
}

@Composable
internal fun rememberCatalogEventLog(): CatalogEventLog = remember { CatalogEventLog() }

@Composable
internal fun CatalogEventLogColumn(log: CatalogEventLog) {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.xxs)) {
        CatalogSectionLabel("event log")
        for (line in log.lines) {
            Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun CatalogHapticLogColumn(entries: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.xxs)) {
        CatalogSectionLabel("last haptics")
        for (entry in entries.take(HAPTIC_LOG_DISPLAY_COUNT)) {
            Text(entry, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private const val HAPTIC_LOG_DISPLAY_COUNT = 4

/** `keypad-weight`: a kg/lb toggle in the page header drives [CatalogWeightUnit.max]/step options. */
@Composable
internal fun KeypadWeightCatalogPage() {
    var unit by remember { mutableStateOf(CatalogWeightUnit.Kg) }
    var stepIndex by remember { mutableStateOf(DEFAULT_WEIGHT_STEP_INDEX) }
    var value by remember(unit) { mutableStateOf<FixedDecimal?>(unit.sample) }
    val last = unit.sample
    val symbols = rememberRepzNumberSymbols()
    val eventLog = rememberCatalogEventLog()
    val platformPerformer = rememberPlatformHapticPerformer()
    val hapticLog = remember(platformPerformer) { CatalogHapticLog(platformPerformer) }
    val step = unit.stepOptions[stepIndex % unit.stepOptions.size]

    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
            Button(onClick = { unit = CatalogWeightUnit.Kg }, enabled = unit != CatalogWeightUnit.Kg) { Text(CatalogWeightUnit.Kg.label) }
            Button(onClick = { unit = CatalogWeightUnit.Lb }, enabled = unit != CatalogWeightUnit.Lb) { Text(CatalogWeightUnit.Lb.label) }
        }
        CompositionLocalProvider(LocalRepzHaptics provides RepzHaptics(hapticLog, enabled = true)) {
            NumberKeypadSheet(
                request =
                    NumberKeypadRequest(
                        targetKey = "catalog-weight:${unit.name}",
                        field = catalogWeightField(unit, step, value, last),
                        modes = listOf(KeypadMode.Weight, KeypadMode.Reps, KeypadMode.Rpe),
                        weightUnitLabel = unit.label,
                        contextLabel = CatalogSpecimens.KEYPAD_CONTEXT_LABEL,
                        nextAction = KeypadNextAction.Reps,
                        stepOptions = unit.stepOptions,
                    ),
                onValueChange = { newValue ->
                    value = newValue
                    eventLog.lastValue = "value: ${newValue?.format(symbols) ?: "(empty)"}"
                },
                onModeSelect = { mode -> eventLog.lastMode = "mode: $mode" },
                onStepSelect = { newStep ->
                    stepIndex = unit.stepOptions.indexOf(newStep).let { if (it == -1) 0 else it }
                    eventLog.lastAction = "step: ${newStep.format(symbols)}"
                },
                onNext = { eventLog.lastAction = "next" },
                onDismiss = { eventLog.lastAction = "dismiss" },
            )
        }
        CatalogEventLogColumn(eventLog)
        CatalogHapticLogColumn(hapticLog.entries)
    }
}

/** `keypad-reps`: sample 8 reps, last 8 (chip visible), suggestions 5/6/8/10/12. */
@Composable
internal fun KeypadRepsCatalogPage() {
    var value by remember { mutableStateOf<FixedDecimal?>(SAMPLE_REPS) }
    val symbols = rememberRepzNumberSymbols()
    val eventLog = rememberCatalogEventLog()
    val platformPerformer = rememberPlatformHapticPerformer()
    val hapticLog = remember(platformPerformer) { CatalogHapticLog(platformPerformer) }

    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.md)) {
        CompositionLocalProvider(LocalRepzHaptics provides RepzHaptics(hapticLog, enabled = true)) {
            NumberKeypadSheet(
                request =
                    NumberKeypadRequest(
                        targetKey = "catalog-reps",
                        field = catalogRepsField(value, SAMPLE_REPS),
                        modes = listOf(KeypadMode.Weight, KeypadMode.Reps, KeypadMode.Rpe),
                        weightUnitLabel = CatalogWeightUnit.Kg.label,
                        contextLabel = CatalogSpecimens.KEYPAD_CONTEXT_LABEL,
                        nextAction = KeypadNextAction.Rpe,
                        stepOptions = emptyList(),
                    ),
                onValueChange = { newValue ->
                    value = newValue
                    eventLog.lastValue = "value: ${newValue?.format(symbols) ?: "(empty)"}"
                },
                onModeSelect = { mode -> eventLog.lastMode = "mode: $mode" },
                onStepSelect = {},
                onNext = { eventLog.lastAction = "next" },
                onDismiss = { eventLog.lastAction = "dismiss" },
            )
        }
        CatalogEventLogColumn(eventLog)
        CatalogHapticLogColumn(hapticLog.entries)
    }
}

/** `keypad-rpe`: sample last 82.5 x 8 @ RPE 8 (plan §3.8), the 9 RPE choices as keys (U2 option a). */
@Composable
internal fun KeypadRpeCatalogPage() {
    var value by remember { mutableStateOf<FixedDecimal?>(SAMPLE_RPE) }
    val symbols = rememberRepzNumberSymbols()
    val eventLog = rememberCatalogEventLog()
    val platformPerformer = rememberPlatformHapticPerformer()
    val hapticLog = remember(platformPerformer) { CatalogHapticLog(platformPerformer) }

    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.md)) {
        CompositionLocalProvider(LocalRepzHaptics provides RepzHaptics(hapticLog, enabled = true)) {
            NumberKeypadSheet(
                request =
                    NumberKeypadRequest(
                        targetKey = "catalog-rpe",
                        field = catalogRpeField(value, SAMPLE_RPE),
                        modes = listOf(KeypadMode.Weight, KeypadMode.Reps, KeypadMode.Rpe),
                        weightUnitLabel = CatalogWeightUnit.Kg.label,
                        contextLabel = CatalogSpecimens.KEYPAD_CONTEXT_LABEL,
                        nextAction = KeypadNextAction.NextSet,
                        stepOptions = emptyList(),
                    ),
                onValueChange = { newValue ->
                    value = newValue
                    eventLog.lastValue = "value: ${newValue?.format(symbols) ?: "(empty)"}"
                },
                onModeSelect = { mode -> eventLog.lastMode = "mode: $mode" },
                onStepSelect = {},
                onNext = { eventLog.lastAction = "next" },
                onDismiss = { eventLog.lastAction = "dismiss" },
            )
        }
        CatalogEventLogColumn(eventLog)
        CatalogHapticLogColumn(hapticLog.entries)
    }
}
