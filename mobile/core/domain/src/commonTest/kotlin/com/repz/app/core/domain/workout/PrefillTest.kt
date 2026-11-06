package com.repz.app.core.domain.workout

import com.repz.app.core.domain.model.SetType
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.model.WorkoutSetId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PrefillTest {
    private fun testSetId(seed: Int) = WorkoutSetId(testUuid(seed))

    @Test
    fun givenNoHistory_whenPrefillingAnAddedExercise_thenOneEmptyNormalSetIsReturned() {
        val sets = prefilledSetsForAddedExercise(lastPerformance = null, newSetId = { testSetId(1) })

        val set = sets.single()
        assertEquals(SetType.NORMAL, set.type)
        assertEquals(Weight.ZERO, set.weight)
        assertEquals(0, set.reps)
        assertNull(set.completedAt)
        assertEquals(testSetId(1), set.id)
    }

    @Test
    fun givenLastPerformance_whenPrefillingAnAddedExercise_thenItIsCopiedPositionByPositionUncompleted() {
        val lastPerformance = listOf(testWorkoutSet(seed = 1, weight = Weight.fromKilograms(80.0), reps = 8))

        val sets = prefilledSetsForAddedExercise(lastPerformance = lastPerformance, newSetId = { testSetId(2) })

        val set = sets.single()
        assertEquals(lastPerformance.single().type, set.type)
        assertEquals(Weight.fromKilograms(80.0), set.weight)
        assertEquals(8, set.reps)
        assertNull(set.completedAt)
        assertEquals(testSetId(2), set.id)
    }

    @Test
    fun givenATemplateWithNoHistoryAndATargetRepsRange_whenPrefillingATemplateExercise_thenRepsIsTheBottomOfTheRange() {
        val target = testTemplateExercise(targetSets = 3, targetReps = 6..8)
        var nextSeed = 1

        val sets = prefilledSetsForTemplateExercise(target, lastPerformance = null) { testSetId(nextSeed++) }

        assertEquals(3, sets.size)
        sets.forEach { set ->
            assertEquals(SetType.NORMAL, set.type)
            assertEquals(Weight.ZERO, set.weight)
            assertEquals(6, set.reps)
            assertNull(set.completedAt)
        }
    }

    @Test
    fun givenATemplateWithNoHistoryAndNoTargetRepsRange_whenPrefillingATemplateExercise_thenRepsIsBlank() {
        val target = testTemplateExercise(targetSets = 1, targetReps = null)

        val sets = prefilledSetsForTemplateExercise(target, lastPerformance = null) { testSetId(1) }

        assertEquals(0, sets.single().reps)
    }

    @Test
    fun givenATemplateWithHistory_whenPrefillingATemplateExercise_thenEachSetTakesItsPositionsLastPerformance() {
        val target = testTemplateExercise(targetSets = 2, targetReps = 6..8)
        val lastPerformance = listOf(testWorkoutSet(seed = 1, weight = Weight.fromKilograms(60.0), reps = 7))
        var nextSeed = 1

        val sets = prefilledSetsForTemplateExercise(target, lastPerformance) { testSetId(nextSeed++) }

        assertEquals(Weight.fromKilograms(60.0), sets[0].weight)
        assertEquals(7, sets[0].reps)
        assertEquals(Weight.ZERO, sets[1].weight)
        assertEquals(6, sets[1].reps)
    }

    @Test
    fun givenLastPerformanceAtThatPosition_whenAppendingASet_thenItIsCopiedUncompleted() {
        val exercise = testWorkoutExercise(seed = 1, sets = listOf(testWorkoutSet(seed = 1)))
        val lastPerformance = listOf(testWorkoutSet(seed = 10), testWorkoutSet(seed = 11, weight = Weight.fromKilograms(90.0), reps = 6))

        val appended = prefilledSetToAppend(exercise, lastPerformance, testSetId(99))

        assertEquals(Weight.fromKilograms(90.0), appended.weight)
        assertEquals(6, appended.reps)
        assertNull(appended.completedAt)
        assertEquals(testSetId(99), appended.id)
    }

    @Test
    fun givenNoLastPerformanceAtThatPosition_whenAppendingASet_thenItCopiesTheExercisesCurrentLastSet() {
        val lastSet = testWorkoutSet(seed = 1, weight = Weight.fromKilograms(70.0), reps = 10)
        val exercise = testWorkoutExercise(seed = 1, sets = listOf(lastSet))

        val appended = prefilledSetToAppend(exercise, lastPerformance = null, testSetId(2))

        assertEquals(Weight.fromKilograms(70.0), appended.weight)
        assertEquals(10, appended.reps)
    }

    @Test
    fun givenNoHistoryAtAllAndAnEmptyExercise_whenAppendingASet_thenItIsBlank() {
        val exercise = testWorkoutExercise(seed = 1, sets = emptyList())

        val appended = prefilledSetToAppend(exercise, lastPerformance = null, testSetId(1))

        assertEquals(Weight.ZERO, appended.weight)
        assertEquals(0, appended.reps)
    }
}
