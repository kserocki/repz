package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.repz.app.core.designsystem.format.FixedDecimal
import com.repz.app.core.designsystem.format.rememberRepzNumberSymbols
import com.repz.app.core.designsystem.haptics.LocalRepzHaptics
import com.repz.app.core.designsystem.haptics.RepzHaptics
import com.repz.app.core.designsystem.haptics.rememberPlatformHapticPerformer
import com.repz.app.core.designsystem.keypad.KeypadField
import com.repz.app.core.designsystem.keypad.KeypadMode
import com.repz.app.core.designsystem.keypad.KeypadNextAction
import com.repz.app.core.designsystem.keypad.NumberKeypadRequest
import com.repz.app.core.designsystem.keypad.NumberKeypadSheet
import com.repz.app.core.designsystem.theme.RepzSpacing

/** A selectable kg/lb control: both options stay enabled, and the current unit is drawn selected
 * and exposed as `selected`, the same pattern as the sheet's own mode row. */
@Composable
private fun CatalogWeightUnitToggle(
    selected: CatalogWeightUnit,
    onSelect: (CatalogWeightUnit) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.xs)) {
        for (unit in CatalogWeightUnit.entries) {
            val isSelected = unit == selected
            val containerColor =
                if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
            val contentColor =
                if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                modifier =
                    Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(containerColor)
                        .clickable(role = Role.Button, onClick = { onSelect(unit) })
                        .semantics { this.selected = isSelected }
                        .padding(horizontal = RepzSpacing.md, vertical = RepzSpacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(unit.label, style = MaterialTheme.typography.labelLarge, color = contentColor)
            }
        }
    }
}

/** The matching field for [mode], from each mode's own remembered value. */
private fun catalogFieldFor(
    mode: KeypadMode,
    unit: CatalogWeightUnit,
    step: FixedDecimal,
    weightValue: FixedDecimal?,
    repsValue: FixedDecimal?,
    rpeValue: FixedDecimal?,
): KeypadField =
    when (mode) {
        KeypadMode.Weight -> catalogWeightField(unit, step, weightValue, unit.sample)
        KeypadMode.Reps -> catalogRepsField(repsValue, SAMPLE_REPS)
        KeypadMode.Rpe -> catalogRpeField(rpeValue, SAMPLE_RPE)
    }

/** What Next does from [mode]: weight -> reps -> RPE -> next set. */
private fun catalogNextActionFor(mode: KeypadMode): KeypadNextAction =
    when (mode) {
        KeypadMode.Weight -> KeypadNextAction.Reps
        KeypadMode.Reps -> KeypadNextAction.Rpe
        KeypadMode.Rpe -> KeypadNextAction.NextSet
    }

/**
 * Shared body for the three interactive catalog pages: keeps the selected mode in state and
 * builds the matching field, the way a real ViewModel will. A mode tap re-renders the sheet in
 * that mode and resets the buffer (the `targetKey + mode` keying).
 */
@Composable
private fun InteractiveKeypadCatalogPage(
    initialMode: KeypadMode,
    showUnitToggle: Boolean,
) {
    var unit by remember { mutableStateOf(CatalogWeightUnit.Kg) }
    var stepIndex by remember { mutableStateOf(DEFAULT_WEIGHT_STEP_INDEX) }
    var mode by remember { mutableStateOf(initialMode) }
    var weightValue by remember(unit) { mutableStateOf<FixedDecimal?>(unit.sample) }
    var repsValue by remember { mutableStateOf<FixedDecimal?>(SAMPLE_REPS) }
    var rpeValue by remember { mutableStateOf<FixedDecimal?>(SAMPLE_RPE) }
    val symbols = rememberRepzNumberSymbols()
    val eventLog = rememberCatalogEventLog()
    val platformPerformer = rememberPlatformHapticPerformer()
    val hapticLog = remember(platformPerformer) { CatalogHapticLog(platformPerformer) }
    val step = unit.stepOptions[stepIndex % unit.stepOptions.size]

    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.md)) {
        if (showUnitToggle) {
            CatalogWeightUnitToggle(selected = unit, onSelect = { unit = it })
        }
        CompositionLocalProvider(LocalRepzHaptics provides RepzHaptics(hapticLog, enabled = true)) {
            NumberKeypadSheet(
                request =
                    NumberKeypadRequest(
                        targetKey = "catalog-${initialMode.name.lowercase()}:${unit.name}",
                        field = catalogFieldFor(mode, unit, step, weightValue, repsValue, rpeValue),
                        modes = listOf(KeypadMode.Weight, KeypadMode.Reps, KeypadMode.Rpe),
                        weightUnitLabel = unit.label,
                        contextLabel = CatalogSpecimens.KEYPAD_CONTEXT_LABEL,
                        nextAction = catalogNextActionFor(mode),
                        stepOptions = if (mode == KeypadMode.Weight) unit.stepOptions else emptyList(),
                    ),
                onValueChange = { newValue ->
                    when (mode) {
                        KeypadMode.Weight -> weightValue = newValue
                        KeypadMode.Reps -> repsValue = newValue
                        KeypadMode.Rpe -> rpeValue = newValue
                    }
                    eventLog.lastValue = "value: ${newValue?.format(symbols) ?: "(empty)"}"
                },
                onModeSelect = { newMode ->
                    mode = newMode
                    eventLog.lastMode = "mode: $newMode"
                },
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

/** `keypad-weight`: a kg/lb toggle in the page header drives [CatalogWeightUnit.max]/step options. */
@Composable
internal fun KeypadWeightCatalogPage() {
    InteractiveKeypadCatalogPage(initialMode = KeypadMode.Weight, showUnitToggle = true)
}

/** `keypad-reps`: sample 8 reps, last 8 (chip visible), suggestions 5/6/8/10/12. */
@Composable
internal fun KeypadRepsCatalogPage() {
    InteractiveKeypadCatalogPage(initialMode = KeypadMode.Reps, showUnitToggle = false)
}

/** `keypad-rpe`: sample last 82.5 x 8 @ RPE 8 (plan §3.8), the 9 RPE choices as keys (U2 option a). */
@Composable
internal fun KeypadRpeCatalogPage() {
    InteractiveKeypadCatalogPage(initialMode = KeypadMode.Rpe, showUnitToggle = false)
}
