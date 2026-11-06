package com.repz.app.core.ui.keypad

import com.repz.app.core.designsystem.format.FixedDecimal
import com.repz.app.core.designsystem.keypad.KeypadField
import com.repz.app.core.designsystem.keypad.KeypadMode
import com.repz.app.core.designsystem.keypad.KeypadQuickValues
import com.repz.app.core.designsystem.keypad.NumberKeypadRequest
import com.repz.app.core.domain.model.Rpe
import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.preferences.WeightStep
import com.repz.app.core.domain.validation.ValidationLimits
import com.repz.app.core.ui.format.WeightFormat

private const val WEIGHT_FRACTION_DIGITS = 2
private const val REPS_FRACTION_DIGITS = 0
private const val RPE_FRACTION_DIGITS = 1
private val REPS_STEP = FixedDecimal.ofWhole(1)
private val RPE_STEP = FixedDecimal(50)
private val NON_RPE_MODES = listOf(KeypadMode.Weight, KeypadMode.Reps)
private val ALL_MODES = NON_RPE_MODES + KeypadMode.Rpe

/**
 * Pure mapping from the workout feature's [SetValueKeypadRequest] to the sheet's domain-free
 * [NumberKeypadRequest] (plan §3.6/§8). [weightUnitLabel] is resolved by the caller
 * ([SetValueKeypad], a composable) so this function stays plain Kotlin and unit-testable.
 */
internal fun SetValueKeypadRequest.toNumberKeypadRequest(weightUnitLabel: String): NumberKeypadRequest =
    NumberKeypadRequest(
        targetKey = "$targetKey:$field",
        field = toKeypadField(),
        modes = if (isRpeEnabled) ALL_MODES else NON_RPE_MODES,
        weightUnitLabel = weightUnitLabel,
        contextLabel = contextLabel,
        nextAction = nextAction,
        stepOptions = if (field == SetValueField.WEIGHT) weightStepOptions(unit) else emptyList(),
    )

private fun SetValueKeypadRequest.toKeypadField(): KeypadField =
    when (field) {
        SetValueField.WEIGHT -> weightField()
        SetValueField.REPS -> repsField()
        SetValueField.RPE -> rpeField()
    }

private fun SetValueKeypadRequest.weightField(): KeypadField {
    val min = FixedDecimal.ZERO
    val max = WeightFormat.maxSetWeight(unit)
    val step = weightStep.toFixedDecimal()
    val value = current.weight?.let { WeightFormat.toDisplay(it, unit) }
    val sameAsLast = last?.weight?.let { WeightFormat.toDisplay(it, unit) }
    return KeypadField(
        mode = KeypadMode.Weight,
        value = value,
        min = min,
        max = max,
        step = step,
        fractionDigits = WEIGHT_FRACTION_DIGITS,
        sameAsLast = sameAsLast,
        // T8: base is the Same-as-last value, else the current value, else none.
        quickValues = KeypadQuickValues.weightSuggestions(base = sameAsLast ?: value, step = step, min = min, max = max),
        choices = emptyList(),
    )
}

private fun SetValueKeypadRequest.repsField(): KeypadField {
    val max = FixedDecimal.ofWhole(ValidationLimits.REPS_RANGE.last.toLong())
    return KeypadField(
        mode = KeypadMode.Reps,
        value = current.reps?.toFixedDecimal(),
        min = FixedDecimal.ZERO,
        max = max,
        step = REPS_STEP,
        fractionDigits = REPS_FRACTION_DIGITS,
        sameAsLast = last?.reps?.toFixedDecimal(),
        quickValues = KeypadQuickValues.repsSuggestions,
        choices = emptyList(),
    )
}

private fun SetValueKeypadRequest.rpeField(): KeypadField {
    val choices = Rpe.ALL.map { it.toFixedDecimal() }
    return KeypadField(
        mode = KeypadMode.Rpe,
        value = current.rpe?.toFixedDecimal(),
        min = choices.first(),
        max = choices.last(),
        step = RPE_STEP,
        fractionDigits = RPE_FRACTION_DIGITS,
        sameAsLast = last?.rpe?.toFixedDecimal(),
        quickValues = emptyList(),
        choices = choices,
    )
}

private fun weightStepOptions(unit: UnitSystem): List<FixedDecimal> = WeightStep.optionsFor(unit).map { it.toFixedDecimal() }
