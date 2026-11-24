package com.repz.app.core.testing.workout

import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.Workout
import com.repz.app.core.domain.model.WorkoutId
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import com.repz.app.core.domain.workout.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * A hand-written fake (N7): an in-memory store keyed by [Workout.id], tombstones included.
 * [saveFinishedWorkoutResult] is returned as-is, without writing, when it is a configured
 * [DomainResult.Failure]; every write/delete/undo/confirm call is recorded for assertions.
 */
class FakeWorkoutRepository(
    initial: List<Workout> = emptyList(),
    var saveFinishedWorkoutResult: DomainResult<Unit, ValidationError>? = null,
) : WorkoutRepository {
    private data class Entry(
        val workout: Workout,
        val isDeleted: Boolean = false,
    )

    private val entries = MutableStateFlow(initial.associate { it.id to Entry(it) })

    val saveFinishedWorkoutCalls = mutableListOf<Workout>()
    val deleteWorkoutCalls = mutableListOf<WorkoutId>()
    val undoWorkoutDeletionCalls = mutableListOf<WorkoutId>()
    val confirmWorkoutDeletionCalls = mutableListOf<WorkoutId>()

    override fun observeActiveWorkout(): Flow<Workout?> =
        entries.map { current -> current.values.firstOrNull { !it.workout.isFinished }?.workout }

    override fun observeFinishedWorkouts(): Flow<List<Workout>> =
        entries.map { current ->
            current.values
                .filter { it.workout.isFinished && !it.isDeleted }
                .map { it.workout }
                .sortedByDescending { it.startedAt }
        }

    override fun observeWorkout(id: WorkoutId): Flow<Workout?> =
        entries.map { current -> current[id]?.takeUnless { it.isDeleted }?.workout }

    override suspend fun findWorkoutIncludingDeleted(id: WorkoutId): Workout? = entries.value[id]?.workout

    override suspend fun findFinishedWorkouts(): List<Workout> =
        entries.value.values
            .filter { it.workout.isFinished && !it.isDeleted }
            .map { it.workout }

    override suspend fun findFinishedWorkoutsContaining(exerciseIds: Set<ExerciseId>): List<Workout> =
        findFinishedWorkouts().filter { workout -> workout.exercises.any { it.exerciseId in exerciseIds } }

    override suspend fun saveActiveWorkout(workout: Workout) {
        entries.update { it + (workout.id to Entry(workout)) }
    }

    override suspend fun discardActiveWorkout(id: WorkoutId) {
        entries.update { current -> if (current[id]?.workout?.isFinished == false) current - id else current }
    }

    override suspend fun saveFinishedWorkout(workout: Workout): DomainResult<Unit, ValidationError> {
        saveFinishedWorkoutCalls += workout
        val configured = saveFinishedWorkoutResult
        if (configured is DomainResult.Failure) return configured
        entries.update { it + (workout.id to Entry(workout)) }
        return DomainResult.Success(Unit)
    }

    override suspend fun deleteWorkout(id: WorkoutId) {
        deleteWorkoutCalls += id
        entries.update { current -> current[id]?.let { current + (id to it.copy(isDeleted = true)) } ?: current }
    }

    override suspend fun undoWorkoutDeletion(id: WorkoutId) {
        undoWorkoutDeletionCalls += id
        entries.update { current -> current[id]?.let { current + (id to it.copy(isDeleted = false)) } ?: current }
    }

    override suspend fun confirmWorkoutDeletion(id: WorkoutId) {
        confirmWorkoutDeletionCalls += id
    }
}
