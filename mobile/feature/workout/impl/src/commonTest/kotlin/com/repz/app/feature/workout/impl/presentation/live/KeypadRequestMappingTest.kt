package com.repz.app.feature.workout.impl.presentation.live

import com.repz.app.core.designsystem.keypad.KeypadNextAction
import com.repz.app.core.domain.model.SetType
import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.model.WorkoutSetId
import com.repz.app.core.domain.preferences.WeightStep
import com.repz.app.core.ui.keypad.SetValueField
import com.repz.app.core.ui.keypad.SetValues
import com.repz.app.feature.workout.impl.testUuid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

private const val CONTEXT_LABEL = "Bench Press · Set 3"

private fun target(
    field: SetField = SetField.Weight,
    current: LiveSetValuesUi = LiveSetValuesUi(weight = Weight.fromKilograms(82.5), reps = 8, rpe = null),
    last: LiveSetValuesUi? = LiveSetValuesUi(weight = Weight.fromKilograms(80.0), reps = 7, rpe = null),
    isLastSetOfWorkout: Boolean = false,
) = LiveKeypadTargetUi(
    setId = WorkoutSetId(testUuid(1)),
    field = field,
    exerciseName = "Bench Press",
    normalSetNumber = 3,
    setType = SetType.NORMAL,
    current = current,
    last = last,
    isLastSetOfWorkout = isLastSetOfWorkout,
)

/** [LiveKeypadTargetUi.toKeypadRequest] and the [SetField]/[SetValueField] round trip (keypad plan §8). */
class KeypadRequestMappingTest {
    @Test
    fun givenAKeypadTarget_whenMapped_thenTheRequestCarriesTheSetIdFieldAndContext() {
        val keypadTarget = target()

        val request = keypadTarget.toKeypadRequest(CONTEXT_LABEL, UnitSystem.KG, WeightStep.KG_2_5)

        assertEquals(keypadTarget.setId.toString(), request.targetKey)
        assertEquals(SetValueField.WEIGHT, request.field)
        assertEquals(UnitSystem.KG, request.unit)
        assertEquals(WeightStep.KG_2_5, request.weightStep)
        assertEquals(CONTEXT_LABEL, request.contextLabel)
    }

    @Test
    fun givenAKeypadTarget_whenMapped_thenRpeIsAlwaysDisabled() {
        val request = target().toKeypadRequest(CONTEXT_LABEL, UnitSystem.KG, WeightStep.KG_2_5)

        assertFalse(request.isRpeEnabled)
    }

    @Test
    fun givenCurrentAndLastValues_whenMapped_thenTheyCarryOverAsSetValues() {
        val keypadTarget =
            target(
                current = LiveSetValuesUi(weight = Weight.fromKilograms(82.5), reps = 8, rpe = null),
                last = LiveSetValuesUi(weight = Weight.fromKilograms(80.0), reps = 7, rpe = null),
            )

        val request = keypadTarget.toKeypadRequest(CONTEXT_LABEL, UnitSystem.KG, WeightStep.KG_2_5)

        assertEquals(SetValues(weight = Weight.fromKilograms(82.5), reps = 8, rpe = null), request.current)
        assertEquals(SetValues(weight = Weight.fromKilograms(80.0), reps = 7, rpe = null), request.last)
    }

    @Test
    fun givenNoSameAsLast_whenMapped_thenLastIsNull() {
        val request = target(last = null).toKeypadRequest(CONTEXT_LABEL, UnitSystem.KG, WeightStep.KG_2_5)

        assertNull(request.last)
    }

    @Test
    fun givenTheWeightField_whenMapped_thenNextActionIsReps() {
        val request =
            target(field = SetField.Weight, isLastSetOfWorkout = true).toKeypadRequest(CONTEXT_LABEL, UnitSystem.KG, WeightStep.KG_2_5)

        assertEquals(KeypadNextAction.Reps, request.nextAction)
    }

    @Test
    fun givenTheRepsFieldOfANonLastSet_whenMapped_thenNextActionIsNextSet() {
        val request =
            target(field = SetField.Reps, isLastSetOfWorkout = false).toKeypadRequest(CONTEXT_LABEL, UnitSystem.KG, WeightStep.KG_2_5)

        assertEquals(KeypadNextAction.NextSet, request.nextAction)
    }

    @Test
    fun givenTheRepsFieldOfTheWorkoutsLastSet_whenMapped_thenNextActionIsDone() {
        val request =
            target(field = SetField.Reps, isLastSetOfWorkout = true).toKeypadRequest(CONTEXT_LABEL, UnitSystem.KG, WeightStep.KG_2_5)

        assertEquals(KeypadNextAction.Done, request.nextAction)
    }

    @Test
    fun givenAWeightRequestInLb_whenMapped_thenTheUnitAndStepAreInLb() {
        val request = target().toKeypadRequest(CONTEXT_LABEL, UnitSystem.LB, WeightStep.LB_5)

        assertEquals(UnitSystem.LB, request.unit)
        assertEquals(WeightStep.LB_5, request.weightStep)
    }

    @Test
    fun givenEveryFeatureField_whenConvertedToASetValueField_thenItMatchesTheCorrespondingKeypadField() {
        assertEquals(SetValueField.WEIGHT, SetField.Weight.toSetValueField())
        assertEquals(SetValueField.REPS, SetField.Reps.toSetValueField())
        assertEquals(SetValueField.RPE, SetField.Rpe.toSetValueField())
    }

    @Test
    fun givenEveryKeypadField_whenConvertedToAFeatureField_thenItMatchesTheCorrespondingField() {
        assertEquals(SetField.Weight, SetValueField.WEIGHT.toSetField())
        assertEquals(SetField.Reps, SetValueField.REPS.toSetField())
        assertEquals(SetField.Rpe, SetValueField.RPE.toSetField())
    }

    @Test
    fun givenAFeatureField_whenRoundTripped_thenItReturnsToTheSameField() {
        SetField.entries.forEach { field ->
            assertEquals(field, field.toSetValueField().toSetField())
        }
    }
}
