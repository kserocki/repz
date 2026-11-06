package com.repz.app.core.domain.validation

import com.repz.app.core.domain.model.Equipment
import com.repz.app.core.domain.model.Exercise
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.ExerciseOrigin
import com.repz.app.core.domain.model.Muscle
import com.repz.app.core.domain.model.TemplateExercise
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.model.WorkoutTemplate
import com.repz.app.core.domain.model.WorkoutTemplateId
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import com.repz.app.core.domain.workout.testFinishedWorkout
import com.repz.app.core.domain.workout.testUuid
import com.repz.app.core.domain.workout.testWorkoutExercise
import com.repz.app.core.domain.workout.testWorkoutSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private fun testTemplateExercise(
    seed: Int = 1,
    targetSets: Int = 3,
    targetReps: IntRange? = 8..12,
    rest: Duration = 90.seconds,
): TemplateExercise =
    TemplateExercise(
        exerciseId = ExerciseId(testUuid(seed)),
        targetSets = targetSets,
        targetReps = targetReps,
        rest = rest,
    )

private fun testWorkoutTemplate(
    name: String = "Push day",
    notes: String = "",
    exercises: List<TemplateExercise> = listOf(testTemplateExercise()),
): WorkoutTemplate =
    WorkoutTemplate(
        id = WorkoutTemplateId(testUuid(1)),
        name = name,
        notes = notes,
        exercises = exercises,
    )

private fun testCustomExercise(
    name: String = "My exercise",
    primaryMuscle: Muscle = Muscle.CHEST,
    secondaryMuscles: Set<Muscle> = setOf(Muscle.TRICEPS),
): Exercise =
    Exercise(
        id = ExerciseId(testUuid(1)),
        name = name,
        primaryMuscle = primaryMuscle,
        secondaryMuscles = secondaryMuscles,
        equipment = Equipment.BARBELL,
        origin = ExerciseOrigin.Custom,
        translations = emptyList(),
    )

class AggregateValidationTest {
    // --- validateFinishedWorkout ---

    @Test
    fun givenAValidFinishedWorkout_whenValidated_thenItSucceeds() {
        assertEquals(DomainResult.Success(Unit), validateFinishedWorkout(testFinishedWorkout()))
    }

    @Test
    fun givenAnActiveWorkout_whenValidatedAsFinished_thenItThrows() {
        assertFailsWith<IllegalArgumentException> { validateFinishedWorkout(testFinishedWorkout(finishedAt = null)) }
    }

    @Test
    fun givenABlankName_whenValidatingAFinishedWorkout_thenFailsWithNameBlank() {
        assertEquals(DomainResult.Failure(ValidationError.NameBlank), validateFinishedWorkout(testFinishedWorkout(name = "   ")))
    }

    @Test
    fun givenANameOverTheLimit_whenValidatingAFinishedWorkout_thenFailsWithNameTooLong() {
        val name = "a".repeat(ValidationLimits.NAME_MAX_LENGTH + 1)

        assertEquals(
            DomainResult.Failure(ValidationError.NameTooLong(ValidationLimits.NAME_MAX_LENGTH)),
            validateFinishedWorkout(testFinishedWorkout(name = name)),
        )
    }

    @Test
    fun givenNotesOverTheLimit_whenValidatingAFinishedWorkout_thenFailsWithNotesTooLong() {
        val notes = "a".repeat(ValidationLimits.NOTES_MAX_LENGTH + 1)

        assertEquals(
            DomainResult.Failure(ValidationError.NotesTooLong(ValidationLimits.NOTES_MAX_LENGTH)),
            validateFinishedWorkout(testFinishedWorkout(notes = notes)),
        )
    }

    @Test
    fun givenFinishedBeforeStarted_whenValidatingAFinishedWorkout_thenFailsWithFinishedBeforeStarted() {
        val workout =
            testFinishedWorkout(startedAt = Instant.fromEpochSeconds(100), finishedAt = Instant.fromEpochSeconds(50))

        assertEquals(DomainResult.Failure(ValidationError.FinishedBeforeStarted), validateFinishedWorkout(workout))
    }

    @Test
    fun givenFinishedAtEqualToStartedAt_whenValidatingAFinishedWorkout_thenItSucceeds() {
        val sameInstant = Instant.fromEpochSeconds(100)

        assertEquals(
            DomainResult.Success(Unit),
            validateFinishedWorkout(testFinishedWorkout(startedAt = sameInstant, finishedAt = sameInstant)),
        )
    }

    @Test
    fun givenExactlyTheMaximumExercises_whenValidatingAFinishedWorkout_thenItSucceeds() {
        val exercises = (1..ValidationLimits.EXERCISES_PER_WORKOUT_MAX).map { testWorkoutExercise(seed = it) }

        assertEquals(DomainResult.Success(Unit), validateFinishedWorkout(testFinishedWorkout(exercises = exercises)))
    }

    @Test
    fun givenOneExerciseOverTheMaximum_whenValidatingAFinishedWorkout_thenFailsWithTooManyExercises() {
        val exercises = (1..ValidationLimits.EXERCISES_PER_WORKOUT_MAX + 1).map { testWorkoutExercise(seed = it) }

        assertEquals(
            DomainResult.Failure(ValidationError.TooManyExercises(ValidationLimits.EXERCISES_PER_WORKOUT_MAX)),
            validateFinishedWorkout(testFinishedWorkout(exercises = exercises)),
        )
    }

    @Test
    fun givenAnExerciseWithNotesOverTheLimit_whenValidatingAFinishedWorkout_thenFailsWithNotesTooLong() {
        val notes = "a".repeat(ValidationLimits.NOTES_MAX_LENGTH + 1)
        val workout = testFinishedWorkout(exercises = listOf(testWorkoutExercise(seed = 1, notes = notes)))

        assertEquals(
            DomainResult.Failure(ValidationError.NotesTooLong(ValidationLimits.NOTES_MAX_LENGTH)),
            validateFinishedWorkout(workout),
        )
    }

    @Test
    fun givenAnExerciseWithExactlyTheMaximumSets_whenValidatingAFinishedWorkout_thenItSucceeds() {
        val sets = (1..ValidationLimits.SETS_PER_EXERCISE_MAX).map { testWorkoutSet(seed = it) }
        val workout = testFinishedWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = sets)))

        assertEquals(DomainResult.Success(Unit), validateFinishedWorkout(workout))
    }

    @Test
    fun givenAnExerciseWithOneSetOverTheMaximum_whenValidatingAFinishedWorkout_thenFailsWithTooManySets() {
        val sets = (1..ValidationLimits.SETS_PER_EXERCISE_MAX + 1).map { testWorkoutSet(seed = it) }
        val workout = testFinishedWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = sets)))

        assertEquals(
            DomainResult.Failure(ValidationError.TooManySets(ValidationLimits.SETS_PER_EXERCISE_MAX)),
            validateFinishedWorkout(workout),
        )
    }

    @Test
    fun givenASetOverTheWeightLimit_whenValidatingAFinishedWorkout_thenFailsWithWeightTooHeavy() {
        val overweight = Weight(ValidationLimits.SET_WEIGHT_MAX.grams + 1)
        val workout =
            testFinishedWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(testWorkoutSet(1, weight = overweight)))))

        assertEquals(
            DomainResult.Failure(ValidationError.WeightTooHeavy(ValidationLimits.SET_WEIGHT_MAX)),
            validateFinishedWorkout(workout),
        )
    }

    @Test
    fun givenASetWithRepsOverTheLimit_whenValidatingAFinishedWorkout_thenFailsWithRepsOutOfRange() {
        val workout = testFinishedWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(testWorkoutSet(1, reps = 1000)))))

        assertEquals(
            DomainResult.Failure(ValidationError.RepsOutOfRange(ValidationLimits.REPS_RANGE)),
            validateFinishedWorkout(workout),
        )
    }

    @Test
    fun givenMultipleInvalidTopLevelFields_whenValidatingAFinishedWorkout_thenNameWinsOverFinishedBeforeStarted() {
        val workout =
            testFinishedWorkout(
                name = "   ",
                startedAt = Instant.fromEpochSeconds(100),
                finishedAt = Instant.fromEpochSeconds(50),
            )

        assertEquals(DomainResult.Failure(ValidationError.NameBlank), validateFinishedWorkout(workout))
    }

    @Test
    fun givenFinishedBeforeStartedAndTooManyExercises_whenValidatingAFinishedWorkout_thenFinishedBeforeStartedWins() {
        val exercises = (1..ValidationLimits.EXERCISES_PER_WORKOUT_MAX + 1).map { testWorkoutExercise(seed = it) }
        val workout =
            testFinishedWorkout(
                startedAt = Instant.fromEpochSeconds(100),
                finishedAt = Instant.fromEpochSeconds(50),
                exercises = exercises,
            )

        assertEquals(DomainResult.Failure(ValidationError.FinishedBeforeStarted), validateFinishedWorkout(workout))
    }

    @Test
    fun givenAnExerciseWithInvalidNotesAndTooManySets_whenValidatingAFinishedWorkout_thenNotesWins() {
        val notes = "a".repeat(ValidationLimits.NOTES_MAX_LENGTH + 1)
        val sets = (1..ValidationLimits.SETS_PER_EXERCISE_MAX + 1).map { testWorkoutSet(seed = it) }
        val workout = testFinishedWorkout(exercises = listOf(testWorkoutExercise(seed = 1, notes = notes, sets = sets)))

        assertEquals(
            DomainResult.Failure(ValidationError.NotesTooLong(ValidationLimits.NOTES_MAX_LENGTH)),
            validateFinishedWorkout(workout),
        )
    }

    @Test
    fun givenASetOverweightAndOverReps_whenValidatingAFinishedWorkout_thenWeightWins() {
        val overweight = Weight(ValidationLimits.SET_WEIGHT_MAX.grams + 1)
        val set = testWorkoutSet(seed = 1, weight = overweight, reps = 1000)
        val workout = testFinishedWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(set))))

        assertEquals(
            DomainResult.Failure(ValidationError.WeightTooHeavy(ValidationLimits.SET_WEIGHT_MAX)),
            validateFinishedWorkout(workout),
        )
    }

    // --- validateWorkoutTemplate ---

    @Test
    fun givenAValidTemplate_whenValidated_thenItSucceeds() {
        assertEquals(DomainResult.Success(Unit), validateWorkoutTemplate(testWorkoutTemplate()))
    }

    @Test
    fun givenABlankName_whenValidatingATemplate_thenFailsWithNameBlank() {
        assertEquals(DomainResult.Failure(ValidationError.NameBlank), validateWorkoutTemplate(testWorkoutTemplate(name = "   ")))
    }

    @Test
    fun givenNotesOverTheLimit_whenValidatingATemplate_thenFailsWithNotesTooLong() {
        val notes = "a".repeat(ValidationLimits.NOTES_MAX_LENGTH + 1)

        assertEquals(
            DomainResult.Failure(ValidationError.NotesTooLong(ValidationLimits.NOTES_MAX_LENGTH)),
            validateWorkoutTemplate(testWorkoutTemplate(notes = notes)),
        )
    }

    @Test
    fun givenExactlyTheMaximumExercises_whenValidatingATemplate_thenItSucceeds() {
        val exercises = (1..ValidationLimits.EXERCISES_PER_TEMPLATE_MAX).map { testTemplateExercise(seed = it) }

        assertEquals(DomainResult.Success(Unit), validateWorkoutTemplate(testWorkoutTemplate(exercises = exercises)))
    }

    @Test
    fun givenOneExerciseOverTheMaximum_whenValidatingATemplate_thenFailsWithTooManyExercises() {
        val exercises = (1..ValidationLimits.EXERCISES_PER_TEMPLATE_MAX + 1).map { testTemplateExercise(seed = it) }

        assertEquals(
            DomainResult.Failure(ValidationError.TooManyExercises(ValidationLimits.EXERCISES_PER_TEMPLATE_MAX)),
            validateWorkoutTemplate(testWorkoutTemplate(exercises = exercises)),
        )
    }

    @Test
    fun givenTargetSetsAtTheLowerBoundary_whenValidatingATemplate_thenItSucceeds() {
        assertEquals(
            DomainResult.Success(Unit),
            validateWorkoutTemplate(testWorkoutTemplate(exercises = listOf(testTemplateExercise(targetSets = 1)))),
        )
    }

    @Test
    fun givenTargetSetsAtTheUpperBoundary_whenValidatingATemplate_thenItSucceeds() {
        assertEquals(
            DomainResult.Success(Unit),
            validateWorkoutTemplate(testWorkoutTemplate(exercises = listOf(testTemplateExercise(targetSets = 20)))),
        )
    }

    @Test
    fun givenTargetSetsBelowTheLowerBoundary_whenValidatingATemplate_thenFailsWithTargetSetsOutOfRange() {
        val template = testWorkoutTemplate(exercises = listOf(testTemplateExercise(targetSets = 0)))

        assertEquals(
            DomainResult.Failure(ValidationError.TargetSetsOutOfRange(ValidationLimits.TARGET_SETS_RANGE)),
            validateWorkoutTemplate(template),
        )
    }

    @Test
    fun givenTargetSetsAboveTheUpperBoundary_whenValidatingATemplate_thenFailsWithTargetSetsOutOfRange() {
        val template = testWorkoutTemplate(exercises = listOf(testTemplateExercise(targetSets = 21)))

        assertEquals(
            DomainResult.Failure(ValidationError.TargetSetsOutOfRange(ValidationLimits.TARGET_SETS_RANGE)),
            validateWorkoutTemplate(template),
        )
    }

    @Test
    fun givenNoTargetReps_whenValidatingATemplate_thenItSucceeds() {
        assertEquals(
            DomainResult.Success(Unit),
            validateWorkoutTemplate(testWorkoutTemplate(exercises = listOf(testTemplateExercise(targetReps = null)))),
        )
    }

    @Test
    fun givenTargetRepsAtTheBoundaries_whenValidatingATemplate_thenItSucceeds() {
        assertEquals(
            DomainResult.Success(Unit),
            validateWorkoutTemplate(testWorkoutTemplate(exercises = listOf(testTemplateExercise(targetReps = 1..999)))),
        )
    }

    @Test
    fun givenTargetRepsMinBelowOne_whenValidatingATemplate_thenFailsWithTargetRepsOutOfRange() {
        val template = testWorkoutTemplate(exercises = listOf(testTemplateExercise(targetReps = 0..12)))

        assertEquals(DomainResult.Failure(ValidationError.TargetRepsOutOfRange(1..999)), validateWorkoutTemplate(template))
    }

    @Test
    fun givenTargetRepsMaxAboveNineNinetyNine_whenValidatingATemplate_thenFailsWithTargetRepsOutOfRange() {
        val template = testWorkoutTemplate(exercises = listOf(testTemplateExercise(targetReps = 8..1000)))

        assertEquals(DomainResult.Failure(ValidationError.TargetRepsOutOfRange(1..999)), validateWorkoutTemplate(template))
    }

    @Test
    fun givenTargetRepsMinGreaterThanMax_whenValidatingATemplate_thenFailsWithTargetRepsOutOfRange() {
        // IntRange(12, 8), not the "12..8" literal: detekt's InvalidRange flags that literal as a
        // likely mistake, but an inverted range is exactly the case under test here.
        val template = testWorkoutTemplate(exercises = listOf(testTemplateExercise(targetReps = IntRange(12, 8))))

        assertEquals(DomainResult.Failure(ValidationError.TargetRepsOutOfRange(1..999)), validateWorkoutTemplate(template))
    }

    @Test
    fun givenRestAtTheLowerBoundary_whenValidatingATemplate_thenItSucceeds() {
        assertEquals(
            DomainResult.Success(Unit),
            validateWorkoutTemplate(testWorkoutTemplate(exercises = listOf(testTemplateExercise(rest = 0.seconds)))),
        )
    }

    @Test
    fun givenRestAtTheUpperBoundary_whenValidatingATemplate_thenItSucceeds() {
        assertEquals(
            DomainResult.Success(Unit),
            validateWorkoutTemplate(testWorkoutTemplate(exercises = listOf(testTemplateExercise(rest = 3600.seconds)))),
        )
    }

    @Test
    fun givenRestOverTheUpperBoundary_whenValidatingATemplate_thenFailsWithRestOutOfRange() {
        val template = testWorkoutTemplate(exercises = listOf(testTemplateExercise(rest = 3601.seconds)))

        assertEquals(DomainResult.Failure(ValidationError.RestOutOfRange(ValidationLimits.REST_MAX)), validateWorkoutTemplate(template))
    }

    @Test
    fun givenRestWithASubSecondComponent_whenValidatingATemplate_thenFailsWithRestOutOfRange() {
        val template = testWorkoutTemplate(exercises = listOf(testTemplateExercise(rest = 90.5.seconds)))

        assertEquals(DomainResult.Failure(ValidationError.RestOutOfRange(ValidationLimits.REST_MAX)), validateWorkoutTemplate(template))
    }

    @Test
    fun givenTargetSetsAndTargetRepsBothInvalid_whenValidatingATemplate_thenTargetSetsWins() {
        val template = testWorkoutTemplate(exercises = listOf(testTemplateExercise(targetSets = 0, targetReps = 0..12)))

        assertEquals(
            DomainResult.Failure(ValidationError.TargetSetsOutOfRange(ValidationLimits.TARGET_SETS_RANGE)),
            validateWorkoutTemplate(template),
        )
    }

    @Test
    fun givenTargetRepsAndRestBothInvalid_whenValidatingATemplate_thenTargetRepsWins() {
        val template = testWorkoutTemplate(exercises = listOf(testTemplateExercise(targetReps = 0..12, rest = 3601.seconds)))

        assertEquals(DomainResult.Failure(ValidationError.TargetRepsOutOfRange(1..999)), validateWorkoutTemplate(template))
    }

    // --- validateCustomExercise ---

    @Test
    fun givenAValidCustomExercise_whenValidated_thenItSucceeds() {
        assertEquals(DomainResult.Success(Unit), validateCustomExercise(testCustomExercise()))
    }

    @Test
    fun givenABlankName_whenValidatingACustomExercise_thenFailsWithNameBlank() {
        assertEquals(DomainResult.Failure(ValidationError.NameBlank), validateCustomExercise(testCustomExercise(name = "   ")))
    }

    @Test
    fun givenANameOverTheLimit_whenValidatingACustomExercise_thenFailsWithNameTooLong() {
        val name = "a".repeat(ValidationLimits.NAME_MAX_LENGTH + 1)

        assertEquals(
            DomainResult.Failure(ValidationError.NameTooLong(ValidationLimits.NAME_MAX_LENGTH)),
            validateCustomExercise(testCustomExercise(name = name)),
        )
    }

    @Test
    fun givenThePrimaryMuscleAlsoListedAsSecondary_whenValidatingACustomExercise_thenFailsWithSecondaryMusclesIncludePrimary() {
        val exercise = testCustomExercise(primaryMuscle = Muscle.CHEST, secondaryMuscles = setOf(Muscle.CHEST, Muscle.TRICEPS))

        assertEquals(DomainResult.Failure(ValidationError.SecondaryMusclesIncludePrimary), validateCustomExercise(exercise))
    }

    @Test
    fun givenABlankNameAndThePrimaryMuscleRepeated_whenValidatingACustomExercise_thenNameWins() {
        val exercise = testCustomExercise(name = "   ", primaryMuscle = Muscle.CHEST, secondaryMuscles = setOf(Muscle.CHEST))

        assertEquals(DomainResult.Failure(ValidationError.NameBlank), validateCustomExercise(exercise))
    }
}
