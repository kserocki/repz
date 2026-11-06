package com.repz.app.core.sync.workout

import com.repz.app.core.database.workout.WorkoutAggregate
import com.repz.app.core.database.workout.WorkoutEntity
import com.repz.app.core.database.workout.WorkoutExerciseEntity
import com.repz.app.core.database.workout.WorkoutExerciseWithSets
import com.repz.app.core.database.workout.WorkoutSetEntity
import com.repz.app.core.network.dto.sync.SyncChangeDto
import com.repz.app.core.network.dto.sync.WorkoutExercisePayloadDto
import com.repz.app.core.network.dto.sync.WorkoutPayloadDto
import com.repz.app.core.network.dto.sync.WorkoutSetPayloadDto
import com.repz.app.core.sync.vocabulary.WireVocabulary

/**
 * Builds the push payload for this aggregate, whose workout must already be finished. Children are
 * sorted by their stored position (ADR-0020: list order is the order). Returns `null` when a set's
 * stored [WorkoutSetEntity.type] isn't a recognised wire value - a bug.
 */
internal fun WorkoutAggregate.toUpsertPayload(): WorkoutPayloadDto? {
    val finishedAt = requireNotNull(workout.finishedAt) { "toUpsertPayload: workout ${workout.id} is still active" }
    val exercisePayloads = exercises.sortedBy { it.exercise.position }.map { it.toPayload() ?: return null }
    return WorkoutPayloadDto(
        templateId = workout.templateId,
        name = workout.name,
        notes = workout.notes,
        startedAt = workout.startedAt,
        finishedAt = finishedAt,
        exercises = exercisePayloads,
    )
}

private fun WorkoutExerciseWithSets.toPayload(): WorkoutExercisePayloadDto? {
    val setPayloads = sets.sortedBy { it.position }.map { it.toPayload() ?: return null }
    return WorkoutExercisePayloadDto(
        id = exercise.id,
        exerciseId = exercise.exerciseId,
        notes = exercise.notes,
        sets = setPayloads,
    )
}

private fun WorkoutSetEntity.toPayload(): WorkoutSetPayloadDto? {
    val wireType = WireVocabulary.setTypeFromEntityValue(type) ?: return null
    return WorkoutSetPayloadDto(
        id = id,
        type = wireType,
        weightGrams = weightGrams,
        reps = reps,
        rpe = rpe,
        completedAt = completedAt,
    )
}

/** Maps one pulled workout change to its entity shape, ready for [com.repz.app.core.database.workout.WorkoutDao.saveSyncedWorkout]. */
internal fun SyncChangeDto.Workout.toEntities(): Triple<WorkoutEntity, List<WorkoutExerciseEntity>, List<WorkoutSetEntity>> {
    val entity =
        WorkoutEntity(
            id = id,
            templateId = payload.templateId,
            name = payload.name,
            notes = payload.notes,
            startedAt = payload.startedAt,
            finishedAt = payload.finishedAt,
            updatedAt = updatedAt,
            serverVersion = version,
            isDeleted = false,
        )
    val exercises = mutableListOf<WorkoutExerciseEntity>()
    val sets = mutableListOf<WorkoutSetEntity>()
    payload.exercises.forEachIndexed { exerciseIndex, exercisePayload ->
        exercises +=
            WorkoutExerciseEntity(
                id = exercisePayload.id,
                workoutId = id,
                exerciseId = exercisePayload.exerciseId,
                position = exerciseIndex,
                notes = exercisePayload.notes,
            )
        exercisePayload.sets.forEachIndexed { setIndex, setPayload ->
            sets +=
                WorkoutSetEntity(
                    id = setPayload.id,
                    workoutExerciseId = exercisePayload.id,
                    position = setIndex,
                    type = WireVocabulary.toEntityValue(setPayload.type),
                    weightGrams = setPayload.weightGrams,
                    reps = setPayload.reps,
                    rpe = setPayload.rpe,
                    completedAt = setPayload.completedAt,
                )
        }
    }
    return Triple(entity, exercises, sets)
}
