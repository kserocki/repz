package com.repz.app.feature.workout.impl.presentation.live

import com.repz.app.core.designsystem.keypad.KeypadNextAction
import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.preferences.WeightStep
import com.repz.app.core.ui.keypad.SetValueField
import com.repz.app.core.ui.keypad.SetValueKeypadRequest
import com.repz.app.core.ui.keypad.SetValues

/**
 * Builds the number keypad's request from this screen's own target (ADR-0029):
 * [contextLabel] and [unit]/[weightStep] (the current preferences) are resolved by the caller, a
 * composable. RPE is off until a setting exists, so [SetValueKeypadRequest.isRpeEnabled] is always
 * `false` here.
 */
fun LiveKeypadTargetUi.toKeypadRequest(
    contextLabel: String,
    unit: UnitSystem,
    weightStep: WeightStep,
): SetValueKeypadRequest =
    SetValueKeypadRequest(
        targetKey = setId.toString(),
        field = field.toSetValueField(),
        current = current.toSetValues(),
        last = last?.toSetValues(),
        unit = unit,
        weightStep = weightStep,
        isRpeEnabled = false,
        contextLabel = contextLabel,
        nextAction = field.nextActionTowards(isLastSetOfWorkout),
    )

private fun LiveSetValuesUi.toSetValues(): SetValues = SetValues(weight = weight, reps = reps, rpe = rpe)

/** Weight -> Reps; Reps (or Rpe, once enabled) -> the next set, or Done on the workout's last field. */
private fun SetField.nextActionTowards(isLastSetOfWorkout: Boolean): KeypadNextAction =
    when (this) {
        SetField.Weight -> KeypadNextAction.Reps
        SetField.Reps, SetField.Rpe -> if (isLastSetOfWorkout) KeypadNextAction.Done else KeypadNextAction.NextSet
    }

/** The feature's own field identity, as the keypad's [SetValueField]. */
fun SetField.toSetValueField(): SetValueField =
    when (this) {
        SetField.Weight -> SetValueField.WEIGHT
        SetField.Reps -> SetValueField.REPS
        SetField.Rpe -> SetValueField.RPE
    }

/** The keypad's mode-switch field, back to the feature's own [SetField]. */
fun SetValueField.toSetField(): SetField =
    when (this) {
        SetValueField.WEIGHT -> SetField.Weight
        SetValueField.REPS -> SetField.Reps
        SetValueField.RPE -> SetField.Rpe
    }
