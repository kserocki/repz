package com.repz.app.core.data.workout

import co.touchlab.kermit.Logger
import com.repz.app.core.database.workout.WorkoutAggregate
import com.repz.app.core.database.workout.WorkoutEntity
import com.repz.app.core.database.workout.WorkoutExerciseEntity
import com.repz.app.core.database.workout.WorkoutExerciseWithSets
import com.repz.app.core.database.workout.WorkoutSetEntity
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.Rpe
import com.repz.app.core.domain.model.SetType
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.model.Workout
import com.repz.app.core.domain.model.WorkoutExercise
import com.repz.app.core.domain.model.WorkoutExerciseId
import com.repz.app.core.domain.model.WorkoutId
import com.repz.app.core.domain.model.WorkoutSet
import com.repz.app.core.domain.model.WorkoutSetId
import com.repz.app.core.domain.model.WorkoutTemplateId
import com.repz.app.core.domain.result.DomainResult
import kotlin.time.Instant

/** A corrupt row (R5) is logged and dropped: `null` here, filtered out of a list by the caller. */
internal fun WorkoutAggregate.toDomainOrNull(logger: Logger): Workout? {
    val orderedExercises = exercises.sortedBy { it.exercise.position }.map { it.toDomainOrNull(logger) }
    return if (orderedExercises.any { it == null }) {
        null
    } else {
        Workout(
            id = WorkoutId(workout.id),
            templateId = workout.templateId?.let { WorkoutTemplateId(it) },
            name = workout.name,
            notes = workout.notes,
            startedAt = workout.startedAt,
            finishedAt = workout.finishedAt,
            exercises = orderedExercises.filterNotNull(),
        )
    }
}

internal fun List<WorkoutAggregate>.toDomainList(logger: Logger): List<Workout> = mapNotNull { it.toDomainOrNull(logger) }

private fun WorkoutExerciseWithSets.toDomainOrNull(logger: Logger): WorkoutExercise? {
    val orderedSets = sets.sortedBy { it.position }.map { it.toDomainOrNull(logger) }
    return if (orderedSets.any { it == null }) {
        null
    } else {
        WorkoutExercise(
            id = WorkoutExerciseId(exercise.id),
            exerciseId = ExerciseId(exercise.exerciseId),
            notes = exercise.notes,
            sets = orderedSets.filterNotNull(),
        )
    }
}

private fun WorkoutSetEntity.toDomainOrNull(logger: Logger): WorkoutSet? {
    val setType = type.toSetTypeOrNull(logger)
    val mappedRpe = rpe?.let { Rpe.of(it) }
    val rpeFailed = mappedRpe is DomainResult.Failure
    if (rpeFailed) logger.e { "Invalid rpe $rpe for workout set $id" }
    return if (setType != null && !rpeFailed) {
        WorkoutSet(
            id = WorkoutSetId(id),
            type = setType,
            weight = Weight(weightGrams),
            reps = reps,
            rpe = (mappedRpe as? DomainResult.Success)?.value,
            completedAt = completedAt,
        )
    } else {
        null
    }
}

private fun String.toSetTypeOrNull(logger: Logger): SetType? =
    when (this) {
        "warmup" -> {
            SetType.WARMUP
        }

        "normal" -> {
            SetType.NORMAL
        }

        "drop" -> {
            SetType.DROP
        }

        "failure" -> {
            SetType.FAILURE
        }

        else -> {
            logger.e { "Unknown workout_sets.type value: $this" }
            null
        }
    }

private fun SetType.toWireText(): String =
    when (this) {
        SetType.WARMUP -> "warmup"
        SetType.NORMAL -> "normal"
        SetType.DROP -> "drop"
        SetType.FAILURE -> "failure"
    }

/** [serverVersion]/[isDeleted] are placeholders: [com.repz.app.core.database.workout.WorkoutDao.saveLocalWorkout] overwrites both. */
internal fun Workout.toEntity(updatedAt: Instant): WorkoutEntity =
    WorkoutEntity(
        id = id.value,
        templateId = templateId?.value,
        name = name,
        notes = notes,
        startedAt = startedAt,
        finishedAt = finishedAt,
        updatedAt = updatedAt,
        serverVersion = null,
        isDeleted = false,
    )

internal fun Workout.toExerciseEntities(): List<WorkoutExerciseEntity> =
    exercises.mapIndexed { index, exercise ->
        WorkoutExerciseEntity(
            id = exercise.id.value,
            workoutId = id.value,
            exerciseId = exercise.exerciseId.value,
            position = index,
            notes = exercise.notes,
        )
    }

internal fun Workout.toSetEntities(): List<WorkoutSetEntity> =
    exercises.flatMap { exercise ->
        exercise.sets.mapIndexed { index, set ->
            WorkoutSetEntity(
                id = set.id.value,
                workoutExerciseId = exercise.id.value,
                position = index,
                type = set.type.toWireText(),
                weightGrams = set.weight.grams,
                reps = set.reps,
                rpe = set.rpe?.value,
                completedAt = set.completedAt,
            )
        }
    }
