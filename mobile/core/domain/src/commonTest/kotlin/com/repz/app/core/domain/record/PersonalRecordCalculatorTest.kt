package com.repz.app.core.domain.record

import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.SetType
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.model.Workout
import com.repz.app.core.domain.model.WorkoutExercise
import com.repz.app.core.domain.model.WorkoutExerciseId
import com.repz.app.core.domain.model.WorkoutId
import com.repz.app.core.domain.model.WorkoutSet
import com.repz.app.core.domain.model.WorkoutSetId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlin.uuid.Uuid

private fun testUuid(seed: Int): Uuid = Uuid.parse("00000000-0000-0000-0000-" + seed.toString().padStart(12, '0'))

private fun testSet(
    seed: Int,
    weight: Weight = Weight.fromKilograms(50.0),
    reps: Int = 5,
    type: SetType = SetType.NORMAL,
    completedAt: Instant? = Instant.fromEpochSeconds(seed.toLong()),
): WorkoutSet =
    WorkoutSet(
        id = WorkoutSetId(testUuid(seed)),
        type = type,
        weight = weight,
        reps = reps,
        rpe = null,
        completedAt = completedAt,
    )

private fun testWorkout(
    seed: Int,
    exercises: List<Pair<ExerciseId, List<WorkoutSet>>>,
    isFinished: Boolean = true,
): Workout =
    Workout(
        id = WorkoutId(testUuid(seed)),
        templateId = null,
        name = "Workout",
        notes = "",
        startedAt = Instant.fromEpochSeconds(0),
        finishedAt = if (isFinished) Instant.fromEpochSeconds(3600) else null,
        exercises =
            exercises.mapIndexed { index, (exerciseId, sets) ->
                WorkoutExercise(
                    id = WorkoutExerciseId(testUuid(seed * 1000 + index)),
                    exerciseId = exerciseId,
                    notes = "",
                    sets = sets,
                )
            },
    )

private fun testWorkoutForOneExercise(
    seed: Int,
    exerciseId: ExerciseId,
    sets: List<WorkoutSet>,
    isFinished: Boolean = true,
): Workout = testWorkout(seed, listOf(exerciseId to sets), isFinished)

private val exerciseA = ExerciseId(testUuid(1))
private val exerciseB = ExerciseId(testUuid(2))

private fun List<PersonalRecord>.forExercise(exerciseId: ExerciseId) = filter { it.exerciseId == exerciseId }

private fun List<PersonalRecord>.ofType(type: PersonalRecordType) = single { it.type == type }

class PersonalRecordCalculatorTest {
    @Test
    fun givenNoWorkouts_whenCalculated_thenNoRecords() {
        assertEquals(emptyList(), calculatePersonalRecords(emptyList()))
    }

    @Test
    fun givenAHeavySingleAVolumeSetAndAHighRepSet_whenCalculated_thenEachTypeComesFromItsOwnSet() {
        val heavySingle = testSet(seed = 1, weight = Weight.fromKilograms(140.0), reps = 1)
        val heavyMultiRep = testSet(seed = 2, weight = Weight.fromKilograms(130.0), reps = 5)
        val volumeSet = testSet(seed = 3, weight = Weight.fromKilograms(80.0), reps = 12)
        val highRepSet = testSet(seed = 4, weight = Weight.fromKilograms(20.0), reps = 30)

        val records =
            calculatePersonalRecords(
                listOf(testWorkoutForOneExercise(seed = 1, exerciseA, listOf(heavySingle, heavyMultiRep, volumeSet, highRepSet))),
            ).forExercise(exerciseA)

        assertEquals(heavySingle.id, records.ofType(PersonalRecordType.HEAVIEST).setId)
        assertEquals(heavyMultiRep.id, records.ofType(PersonalRecordType.EST_1RM).setId)
        assertEquals(volumeSet.id, records.ofType(PersonalRecordType.BEST_SET_VOLUME).setId)
        assertEquals(highRepSet.id, records.ofType(PersonalRecordType.MOST_REPS).setId)
    }

    @Test
    fun givenAHeavierWarmup_whenCalculated_thenItNeverWinsHeaviest() {
        val warmup = testSet(seed = 1, weight = Weight.fromKilograms(200.0), reps = 5, type = SetType.WARMUP)
        val working = testSet(seed = 2, weight = Weight.fromKilograms(60.0), reps = 5)

        val records =
            calculatePersonalRecords(listOf(testWorkoutForOneExercise(1, exerciseA, listOf(warmup, working)))).forExercise(exerciseA)

        assertEquals(working.weight, records.ofType(PersonalRecordType.HEAVIEST).weight)
    }

    @Test
    fun givenOnlyADropSet_whenCalculated_thenItCountsTowardsARecord() {
        val drop = testSet(seed = 1, weight = Weight.fromKilograms(60.0), reps = 5, type = SetType.DROP)

        val records = calculatePersonalRecords(listOf(testWorkoutForOneExercise(1, exerciseA, listOf(drop)))).forExercise(exerciseA)

        assertEquals(drop.weight, records.ofType(PersonalRecordType.HEAVIEST).weight)
    }

    @Test
    fun givenOnlyAFailureSet_whenCalculated_thenItCountsTowardsARecord() {
        val failure = testSet(seed = 1, weight = Weight.fromKilograms(55.0), reps = 5, type = SetType.FAILURE)

        val records = calculatePersonalRecords(listOf(testWorkoutForOneExercise(1, exerciseA, listOf(failure)))).forExercise(exerciseA)

        assertEquals(failure.weight, records.ofType(PersonalRecordType.HEAVIEST).weight)
    }

    @Test
    fun givenAnUncompletedSet_whenCalculated_thenItIsExcluded() {
        val uncompleted = testSet(seed = 1, weight = Weight.fromKilograms(200.0), reps = 5, completedAt = null)

        val records = calculatePersonalRecords(listOf(testWorkoutForOneExercise(1, exerciseA, listOf(uncompleted)))).forExercise(exerciseA)

        assertEquals(emptyList(), records)
    }

    @Test
    fun givenAnUnfinishedWorkout_whenCalculated_thenItIsExcluded() {
        val set = testSet(seed = 1, weight = Weight.fromKilograms(200.0), reps = 5)

        val workouts = listOf(testWorkoutForOneExercise(1, exerciseA, listOf(set), isFinished = false))
        val records = calculatePersonalRecords(workouts).forExercise(exerciseA)

        assertEquals(emptyList(), records)
    }

    @Test
    fun givenZeroReps_whenCalculated_thenTheSetIsExcluded() {
        val zeroReps = testSet(seed = 1, weight = Weight.fromKilograms(100.0), reps = 0)

        val records = calculatePersonalRecords(listOf(testWorkoutForOneExercise(1, exerciseA, listOf(zeroReps)))).forExercise(exerciseA)

        assertEquals(emptyList(), records)
    }

    @Test
    fun givenABodyweightExercise_whenCalculated_thenOnlyMostRepsIsRecorded() {
        val bodyweightSet = testSet(seed = 1, weight = Weight.ZERO, reps = 12)

        val records =
            calculatePersonalRecords(listOf(testWorkoutForOneExercise(1, exerciseA, listOf(bodyweightSet)))).forExercise(exerciseA)

        assertEquals(listOf(PersonalRecordType.MOST_REPS), records.map { it.type })
        assertEquals(12, records.single().reps)
    }

    @Test
    fun givenAnEqualLaterValue_whenCalculated_thenTheEarlierSetKeepsTheRecord() {
        val earlier = testSet(seed = 1, weight = Weight.fromKilograms(100.0), reps = 5, completedAt = Instant.fromEpochSeconds(100))
        val later = testSet(seed = 2, weight = Weight.fromKilograms(100.0), reps = 5, completedAt = Instant.fromEpochSeconds(200))

        val records =
            calculatePersonalRecords(listOf(testWorkoutForOneExercise(1, exerciseA, listOf(earlier, later)))).forExercise(exerciseA)

        assertEquals(earlier.id, records.ofType(PersonalRecordType.HEAVIEST).setId)
        assertEquals(earlier.completedAt, records.ofType(PersonalRecordType.HEAVIEST).achievedAt)
    }

    @Test
    fun givenAStrictlyGreaterLaterValue_whenCalculated_thenTheLaterSetWins() {
        val earlier = testSet(seed = 1, weight = Weight.fromKilograms(100.0), reps = 5, completedAt = Instant.fromEpochSeconds(100))
        val later = testSet(seed = 2, weight = Weight.fromKilograms(110.0), reps = 5, completedAt = Instant.fromEpochSeconds(200))

        val records =
            calculatePersonalRecords(listOf(testWorkoutForOneExercise(1, exerciseA, listOf(earlier, later)))).forExercise(exerciseA)

        assertEquals(later.id, records.ofType(PersonalRecordType.HEAVIEST).setId)
    }

    @Test
    fun givenTheSameCompletedAtInDifferentWorkouts_whenCalculated_thenTheSmallerWorkoutIdWins() {
        val sameInstant = Instant.fromEpochSeconds(100)
        val inFirstWorkout = testSet(seed = 10, weight = Weight.fromKilograms(100.0), reps = 5, completedAt = sameInstant)
        val inSecondWorkout = testSet(seed = 20, weight = Weight.fromKilograms(100.0), reps = 5, completedAt = sameInstant)

        val records =
            calculatePersonalRecords(
                listOf(
                    testWorkoutForOneExercise(seed = 10, exerciseA, listOf(inFirstWorkout)),
                    testWorkoutForOneExercise(seed = 20, exerciseA, listOf(inSecondWorkout)),
                ),
            ).forExercise(exerciseA)

        assertEquals(inFirstWorkout.id, records.ofType(PersonalRecordType.HEAVIEST).setId)
    }

    @Test
    fun givenTheSameCompletedAtAndWorkout_whenCalculated_thenTheSmallerSetIdWins() {
        val sameInstant = Instant.fromEpochSeconds(100)
        val smallerSetId = testSet(seed = 10, weight = Weight.fromKilograms(100.0), reps = 5, completedAt = sameInstant)
        val largerSetId = testSet(seed = 20, weight = Weight.fromKilograms(100.0), reps = 5, completedAt = sameInstant)

        val records =
            calculatePersonalRecords(
                listOf(testWorkoutForOneExercise(1, exerciseA, listOf(largerSetId, smallerSetId))),
            ).forExercise(exerciseA)

        assertEquals(smallerSetId.id, records.ofType(PersonalRecordType.HEAVIEST).setId)
    }

    @Test
    fun givenTwoExercises_whenCalculated_thenTheirRecordsStaySeparate() {
        val heavyA = testSet(seed = 1, weight = Weight.fromKilograms(60.0), reps = 5)
        val heavyB = testSet(seed = 2, weight = Weight.fromKilograms(200.0), reps = 5)

        val records =
            calculatePersonalRecords(
                listOf(testWorkout(1, listOf(exerciseA to listOf(heavyA), exerciseB to listOf(heavyB)))),
            )

        assertEquals(heavyA.weight, records.forExercise(exerciseA).ofType(PersonalRecordType.HEAVIEST).weight)
        assertEquals(heavyB.weight, records.forExercise(exerciseB).ofType(PersonalRecordType.HEAVIEST).weight)
    }

    @Test
    fun givenTheSameExerciseTwiceInOneWorkout_whenCalculated_thenItFormsOnePool() {
        val firstEntry = testSet(seed = 1, weight = Weight.fromKilograms(60.0), reps = 5)
        val secondEntry = testSet(seed = 2, weight = Weight.fromKilograms(90.0), reps = 5)

        val records =
            calculatePersonalRecords(
                listOf(testWorkout(1, listOf(exerciseA to listOf(firstEntry), exerciseA to listOf(secondEntry)))),
            ).forExercise(exerciseA)

        assertEquals(secondEntry.weight, records.ofType(PersonalRecordType.HEAVIEST).weight)
    }

    @Test
    fun givenAWinningSet_whenCalculated_thenTheRecordCarriesItsOwnFields() {
        val set = testSet(seed = 1, weight = Weight.fromKilograms(100.0), reps = 7, completedAt = Instant.fromEpochSeconds(500))
        val workout = testWorkoutForOneExercise(seed = 42, exerciseA, listOf(set))

        val record = calculatePersonalRecords(listOf(workout)).forExercise(exerciseA).ofType(PersonalRecordType.HEAVIEST)

        assertEquals(exerciseA, record.exerciseId)
        assertEquals(set.weight, record.weight)
        assertEquals(set.reps, record.reps)
        assertEquals(set.id, record.setId)
        assertEquals(workout.id, record.workoutId)
        assertEquals(set.completedAt, record.achievedAt)
    }

    @Test
    fun givenALoadRecord_whenValueIsRead_thenItWrapsTheWeight() {
        val set = testSet(seed = 1, weight = Weight.fromKilograms(100.0), reps = 7)
        val records = calculatePersonalRecords(listOf(testWorkoutForOneExercise(1, exerciseA, listOf(set)))).forExercise(exerciseA)

        assertEquals(PersonalRecordValue.Load(set.weight), records.ofType(PersonalRecordType.HEAVIEST).value)
    }

    @Test
    fun givenAMostRepsRecord_whenValueIsRead_thenItWrapsTheRepCount() {
        val set = testSet(seed = 1, weight = Weight.fromKilograms(100.0), reps = 7)
        val records = calculatePersonalRecords(listOf(testWorkoutForOneExercise(1, exerciseA, listOf(set)))).forExercise(exerciseA)

        assertEquals(PersonalRecordValue.Reps(7), records.ofType(PersonalRecordType.MOST_REPS).value)
    }

    @Test
    fun givenTwoExercisesWithMultipleTypes_whenCalculated_thenTheOutputIsSortedByExerciseIdThenType() {
        val setA = testSet(seed = 1, weight = Weight.fromKilograms(60.0), reps = 5)
        val setB = testSet(seed = 2, weight = Weight.fromKilograms(70.0), reps = 6)

        // exerciseB is listed first so a naive (unsorted) result would start with its records:
        // only an explicit sort by exerciseId then puts exerciseA's records first.
        val records = calculatePersonalRecords(listOf(testWorkout(1, listOf(exerciseB to listOf(setB), exerciseA to listOf(setA)))))

        val expectedOrder =
            listOf(
                exerciseA to PersonalRecordType.EST_1RM,
                exerciseA to PersonalRecordType.HEAVIEST,
                exerciseA to PersonalRecordType.BEST_SET_VOLUME,
                exerciseA to PersonalRecordType.MOST_REPS,
                exerciseB to PersonalRecordType.EST_1RM,
                exerciseB to PersonalRecordType.HEAVIEST,
                exerciseB to PersonalRecordType.BEST_SET_VOLUME,
                exerciseB to PersonalRecordType.MOST_REPS,
            )
        assertEquals(expectedOrder, records.map { it.exerciseId to it.type })
    }
}
