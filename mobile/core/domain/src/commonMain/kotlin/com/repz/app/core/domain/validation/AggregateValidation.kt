package com.repz.app.core.domain.validation

import com.repz.app.core.domain.model.Exercise
import com.repz.app.core.domain.model.TemplateExercise
import com.repz.app.core.domain.model.Workout
import com.repz.app.core.domain.model.WorkoutExercise
import com.repz.app.core.domain.model.WorkoutSet
import com.repz.app.core.domain.model.WorkoutTemplate
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import com.repz.app.core.domain.result.errorOrNull

/**
 * A template exercise's `targetReps` bounds: a single aggregate's own field, so (unlike
 * `ValidationLimits.validateRest`, shared with preferences) it stays local to this file instead of
 * `ValidationLimits`.
 */
private val TEMPLATE_TARGET_REPS_RANGE: IntRange = 1..999

/** The first non-null error, in declaration order, or `null` when every one of [errors] passed. */
private fun firstOf(vararg errors: ValidationError?): ValidationError? = errors.firstNotNullOfOrNull { it }

private fun ValidationError?.toDomainResult(): DomainResult<Unit, ValidationError> =
    this?.let { DomainResult.Failure(it) } ?: DomainResult.Success(Unit)

/**
 * Validates a finished (or being re-saved, already-finished) workout before it is written and
 * enqueued to the outbox (R6). Checks run in field order and the first failure wins. The active
 * workout is a draft and is never validated; `workout.finishedAt` must already be set.
 */
fun validateFinishedWorkout(workout: Workout): DomainResult<Unit, ValidationError> {
    val finishedAt = requireNotNull(workout.finishedAt) { "A finished workout must have finishedAt set: ${workout.id}" }

    val error =
        firstOf(
            ValidationLimits.validateName(workout.name).errorOrNull(),
            ValidationLimits.validateNotes(workout.notes).errorOrNull(),
            ValidationError.FinishedBeforeStarted.takeIf { finishedAt < workout.startedAt },
            ValidationError
                .TooManyExercises(ValidationLimits.EXERCISES_PER_WORKOUT_MAX)
                .takeIf { workout.exercises.size > ValidationLimits.EXERCISES_PER_WORKOUT_MAX },
        ) ?: workout.exercises.firstNotNullOfOrNull { workoutExerciseError(it) }

    return error.toDomainResult()
}

private fun workoutExerciseError(exercise: WorkoutExercise): ValidationError? =
    firstOf(
        ValidationLimits.validateNotes(exercise.notes).errorOrNull(),
        ValidationError
            .TooManySets(ValidationLimits.SETS_PER_EXERCISE_MAX)
            .takeIf { exercise.sets.size > ValidationLimits.SETS_PER_EXERCISE_MAX },
    ) ?: exercise.sets.firstNotNullOfOrNull { workoutSetError(it) }

private fun workoutSetError(set: WorkoutSet): ValidationError? =
    firstOf(
        ValidationLimits.validateSetWeight(set.weight).errorOrNull(),
        ValidationLimits.validateReps(set.reps).errorOrNull(),
    )

/**
 * Validates a workout template before it is written and enqueued to the outbox (R6). Checks run in
 * field order and the first failure wins.
 */
fun validateWorkoutTemplate(template: WorkoutTemplate): DomainResult<Unit, ValidationError> {
    val error =
        firstOf(
            ValidationLimits.validateName(template.name).errorOrNull(),
            ValidationLimits.validateNotes(template.notes).errorOrNull(),
            ValidationError
                .TooManyExercises(ValidationLimits.EXERCISES_PER_TEMPLATE_MAX)
                .takeIf { template.exercises.size > ValidationLimits.EXERCISES_PER_TEMPLATE_MAX },
        ) ?: template.exercises.firstNotNullOfOrNull { templateExerciseError(it) }

    return error.toDomainResult()
}

private fun templateExerciseError(exercise: TemplateExercise): ValidationError? {
    val targetReps = exercise.targetReps

    return firstOf(
        ValidationError
            .TargetSetsOutOfRange(ValidationLimits.TARGET_SETS_RANGE)
            .takeIf { exercise.targetSets !in ValidationLimits.TARGET_SETS_RANGE },
        ValidationError
            .TargetRepsOutOfRange(TEMPLATE_TARGET_REPS_RANGE)
            .takeIf { targetReps != null && !targetReps.isValidTemplateTargetReps() },
    ) ?: ValidationLimits.validateRest(exercise.rest).errorOrNull()
}

private fun IntRange.isValidTemplateTargetReps(): Boolean =
    first in TEMPLATE_TARGET_REPS_RANGE && last in TEMPLATE_TARGET_REPS_RANGE && first <= last

/**
 * Validates a custom exercise before it is written and enqueued to the outbox (R6). Catalog
 * exercises are read-only (ADR-0020); that precondition is checked where this is called from, not
 * here. Duplicate secondary muscles are already unrepresentable: [Exercise.secondaryMuscles] is a
 * `Set`.
 */
fun validateCustomExercise(exercise: Exercise): DomainResult<Unit, ValidationError> {
    val error =
        firstOf(
            ValidationLimits.validateName(exercise.name).errorOrNull(),
            ValidationError.SecondaryMusclesIncludePrimary.takeIf { exercise.primaryMuscle in exercise.secondaryMuscles },
        )

    return error.toDomainResult()
}
