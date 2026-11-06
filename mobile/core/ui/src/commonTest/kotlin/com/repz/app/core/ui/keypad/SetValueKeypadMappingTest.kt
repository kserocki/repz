package com.repz.app.core.ui.keypad

import com.repz.app.core.designsystem.format.FixedDecimal
import com.repz.app.core.designsystem.keypad.KeypadMode
import com.repz.app.core.designsystem.keypad.KeypadNextAction
import com.repz.app.core.domain.model.Rpe
import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.preferences.WeightStep
import com.repz.app.core.domain.result.DomainResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val CONTEXT_LABEL = "Bench Press · Set 3"
private const val WEIGHT_UNIT_LABEL = "kg"

private fun rpeOf(value: Double) = (Rpe.of(value) as DomainResult.Success).value

private fun weightRequest(
    current: SetValues = SetValues(weight = Weight.fromKilograms(82.5), reps = null, rpe = null),
    last: SetValues? = null,
    unit: UnitSystem = UnitSystem.KG,
    weightStep: WeightStep = WeightStep.KG_2_5,
    isRpeEnabled: Boolean = true,
    field: SetValueField = SetValueField.WEIGHT,
) = SetValueKeypadRequest(
    targetKey = "set-1",
    field = field,
    current = current,
    last = last,
    unit = unit,
    weightStep = weightStep,
    isRpeEnabled = isRpeEnabled,
    contextLabel = CONTEXT_LABEL,
    nextAction = KeypadNextAction.Reps,
)

class SetValueKeypadMappingTest {
    @Test
    fun givenAWeightRequest_whenMapped_thenTheKeypadFieldMatchesTheCurrentValueAndLimits() {
        val request =
            weightRequest(
                current = SetValues(weight = Weight.fromKilograms(82.5), reps = null, rpe = null),
                last = SetValues(weight = Weight.fromKilograms(80.0), reps = null, rpe = null),
            )

        val mapped = request.toNumberKeypadRequest(WEIGHT_UNIT_LABEL)

        assertEquals(KeypadMode.Weight, mapped.field.mode)
        assertEquals(FixedDecimal(8250), mapped.field.value)
        assertEquals(FixedDecimal.ZERO, mapped.field.min)
        assertEquals(FixedDecimal.ofWhole(1000), mapped.field.max)
        assertEquals(FixedDecimal(250), mapped.field.step)
        assertEquals(2, mapped.field.fractionDigits)
        assertEquals(FixedDecimal(8000), mapped.field.sameAsLast)
        assertEquals(emptyList(), mapped.field.choices)
        assertEquals(WEIGHT_UNIT_LABEL, mapped.weightUnitLabel)
        assertEquals(CONTEXT_LABEL, mapped.contextLabel)
        assertEquals(KeypadNextAction.Reps, mapped.nextAction)
    }

    @Test
    fun givenAWeightRequestWithNoSameAsLast_whenMapped_thenQuickValuesAreBasedOnTheCurrentValue() {
        val request =
            weightRequest(
                current = SetValues(weight = Weight.fromKilograms(82.5), reps = null, rpe = null),
                last = null,
            )

        val mapped = request.toNumberKeypadRequest(WEIGHT_UNIT_LABEL)

        assertNull(mapped.field.sameAsLast)
        assertEquals(
            listOf(77.5, 80.0, 85.0, 87.5).map { FixedDecimal((it * 100).toLong()) },
            mapped.field.quickValues,
        )
    }

    @Test
    fun givenAWeightRequestInLb_whenMapped_thenTheMaxAndStepOptionsAreInLb() {
        val request = weightRequest(unit = UnitSystem.LB, weightStep = WeightStep.LB_5)

        val mapped = request.toNumberKeypadRequest("lb")

        assertEquals(FixedDecimal(220_462), mapped.field.max)
        assertEquals(FixedDecimal.ofWhole(5), mapped.field.step)
        assertEquals(listOf(1.0, 2.5, 5.0).map { FixedDecimal((it * 100).toLong()) }, mapped.stepOptions)
    }

    @Test
    fun givenARepsRequest_whenMapped_thenTheKeypadFieldMatchesRepsLimitsAndSuggestions() {
        val request =
            weightRequest(
                current = SetValues(weight = null, reps = 8, rpe = null),
                last = SetValues(weight = null, reps = 10, rpe = null),
                field = SetValueField.REPS,
            )

        val mapped = request.toNumberKeypadRequest(WEIGHT_UNIT_LABEL)

        assertEquals(KeypadMode.Reps, mapped.field.mode)
        assertEquals(FixedDecimal.ofWhole(8), mapped.field.value)
        assertEquals(FixedDecimal.ZERO, mapped.field.min)
        assertEquals(FixedDecimal.ofWhole(999), mapped.field.max)
        assertEquals(FixedDecimal.ofWhole(1), mapped.field.step)
        assertEquals(0, mapped.field.fractionDigits)
        assertEquals(FixedDecimal.ofWhole(10), mapped.field.sameAsLast)
        assertEquals(listOf(5L, 6L, 8L, 10L, 12L).map { FixedDecimal.ofWhole(it) }, mapped.field.quickValues)
        assertEquals(emptyList(), mapped.field.choices)
        assertEquals(emptyList(), mapped.stepOptions, "the Step chip is weight mode only")
    }

    @Test
    fun givenAnRpeRequest_whenMapped_thenTheKeypadFieldOffersAllNineChoices() {
        val currentRpe = rpeOf(8.0)
        val request =
            weightRequest(
                current = SetValues(weight = null, reps = null, rpe = currentRpe),
                field = SetValueField.RPE,
            )

        val mapped = request.toNumberKeypadRequest(WEIGHT_UNIT_LABEL)

        assertEquals(KeypadMode.Rpe, mapped.field.mode)
        assertEquals(FixedDecimal(800), mapped.field.value)
        assertEquals(FixedDecimal(600), mapped.field.min)
        assertEquals(FixedDecimal(1000), mapped.field.max)
        assertEquals(FixedDecimal(50), mapped.field.step)
        assertEquals(1, mapped.field.fractionDigits)
        assertEquals(emptyList(), mapped.field.quickValues)
        assertEquals(
            listOf(600L, 650L, 700L, 750L, 800L, 850L, 900L, 950L, 1000L).map { FixedDecimal(it) },
            mapped.field.choices,
        )
    }

    @Test
    fun givenRpeDisabled_whenMapped_thenOnlyWeightAndRepsModesAreOffered() {
        val request = weightRequest(isRpeEnabled = false)

        val mapped = request.toNumberKeypadRequest(WEIGHT_UNIT_LABEL)

        assertEquals(listOf(KeypadMode.Weight, KeypadMode.Reps), mapped.modes)
    }

    @Test
    fun givenRpeEnabled_whenMapped_thenAllThreeModesAreOffered() {
        val request = weightRequest(isRpeEnabled = true)

        val mapped = request.toNumberKeypadRequest(WEIGHT_UNIT_LABEL)

        assertEquals(listOf(KeypadMode.Weight, KeypadMode.Reps, KeypadMode.Rpe), mapped.modes)
    }

    @Test
    fun givenTheSameSetButADifferentField_whenMapped_thenTheTargetKeyDiffers() {
        val weightTarget = weightRequest(field = SetValueField.WEIGHT).toNumberKeypadRequest(WEIGHT_UNIT_LABEL).targetKey
        val repsTarget = weightRequest(field = SetValueField.REPS).toNumberKeypadRequest(WEIGHT_UNIT_LABEL).targetKey

        assertEquals("set-1:WEIGHT", weightTarget)
        assertEquals("set-1:REPS", repsTarget)
    }

    @Test
    fun givenAWeightAmount_whenConvertedToASetValueEdit_thenItConvertsThroughWeightFormat() {
        val edit = SetValueField.WEIGHT.toSetValueEdit(FixedDecimal(8250), UnitSystem.KG)

        assertEquals(SetValueEdit.Weight(Weight.fromKilograms(82.5)), edit)
    }

    @Test
    fun givenANullWeightAmount_whenConvertedToASetValueEdit_thenTheWeightIsNull() {
        val edit = SetValueField.WEIGHT.toSetValueEdit(null, UnitSystem.KG)

        assertEquals(SetValueEdit.Weight(null), edit)
    }

    @Test
    fun givenARepsAmount_whenConvertedToASetValueEdit_thenItIsTheWholeNumberOfReps() {
        val edit = SetValueField.REPS.toSetValueEdit(FixedDecimal.ofWhole(12), UnitSystem.KG)

        assertEquals(SetValueEdit.Reps(12), edit)
    }

    @Test
    fun givenANullRepsAmount_whenConvertedToASetValueEdit_thenRepsIsNull() {
        val edit = SetValueField.REPS.toSetValueEdit(null, UnitSystem.KG)

        assertEquals(SetValueEdit.Reps(null), edit)
    }

    @Test
    fun givenAnRpeAmount_whenConvertedToASetValueEdit_thenItGoesThroughRpeOf() {
        val edit = SetValueField.RPE.toSetValueEdit(FixedDecimal(850), UnitSystem.KG)

        assertEquals(SetValueEdit.Rpe(rpeOf(8.5)), edit)
    }

    @Test
    fun givenANullRpeAmount_whenConvertedToASetValueEdit_thenRpeIsNull() {
        val edit = SetValueField.RPE.toSetValueEdit(null, UnitSystem.KG)

        assertEquals(SetValueEdit.Rpe(null), edit)
    }

    @Test
    fun givenEveryKeypadMode_whenConvertedToASetValueField_thenItMatchesTheCorrespondingField() {
        assertEquals(SetValueField.WEIGHT, KeypadMode.Weight.toSetValueField())
        assertEquals(SetValueField.REPS, KeypadMode.Reps.toSetValueField())
        assertEquals(SetValueField.RPE, KeypadMode.Rpe.toSetValueField())
    }

    @Test
    fun givenADisplayStepFromTheSheet_whenConvertedToAWeightStep_thenItMatchesTheOptionForThatUnit() {
        assertEquals(WeightStep.KG_1_25, FixedDecimal(125).toWeightStep(UnitSystem.KG))
        assertEquals(WeightStep.LB_1, FixedDecimal.ofWhole(1).toWeightStep(UnitSystem.LB))
    }
}
