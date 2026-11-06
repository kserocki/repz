package com.repz.app.core.domain.workout

import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.Workout
import com.repz.app.core.domain.model.WorkoutId
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import kotlinx.coroutines.flow.Flow

/**
 * Workouts (rule 6: the UI reads only from Room). The active workout is a local-only draft, never
 * validated and never synced; finishing or editing a finished workout is validated and written
 * with its outbox row in one transaction (ADR-0023).
 */
interface WorkoutRepository {
    fun observeActiveWorkout(): Flow<Workout?>

    /** Newest first (`started_at` descending). */
    fun observeFinishedWorkouts(): Flow<List<Workout>>

    fun observeWorkout(id: WorkoutId): Flow<Workout?>

    /** Includes a tombstoned workout, so it can still be resolved during its undo window. */
    suspend fun findWorkoutIncludingDeleted(id: WorkoutId): Workout?

    suspend fun findFinishedWorkouts(): List<Workout>

    suspend fun findFinishedWorkoutsContaining(exerciseIds: Set<ExerciseId>): List<Workout>

    /**
     * Local only, never enters the outbox (ADR-0023). Requires `workout.finishedAt == null`. Not
     * validated: the active workout is a draft.
     */
    suspend fun saveActiveWorkout(workout: Workout)

    /** Hard delete of the never-synced active workout. No-op if [id] is finished or missing. */
    suspend fun discardActiveWorkout(id: WorkoutId)

    /**
     * Finishes or edits a finished workout: validate, then save the entity and enqueue an UPSERT
     * in one write transaction, then request a sync. Requires `workout.finishedAt != null`. Called
     * through [SaveFinishedWorkoutUseCase], which also recomputes personal records; later features
     * call through it, not this method directly.
     */
    suspend fun saveFinishedWorkout(workout: Workout): DomainResult<Unit, ValidationError>

    /**
     * Tombstones [id] and enqueues a DELETE, held for `DELETE_UNDO_WINDOW`. Called through
     * [DeleteWorkoutUseCase], which also recomputes personal records.
     */
    suspend fun deleteWorkout(id: WorkoutId)

    /**
     * Clears the tombstone and enqueues an UPSERT, coalescing the held DELETE away, then requests a
     * sync. Called through [UndoWorkoutDeletionUseCase], which also recomputes personal records.
     */
    suspend fun undoWorkoutDeletion(id: WorkoutId)

    /** The undo snackbar was dismissed: releases the held DELETE, then requests a sync. */
    suspend fun confirmWorkoutDeletion(id: WorkoutId)
}
