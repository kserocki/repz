package com.repz.app.core.testing.exercise

import com.repz.app.core.domain.exercise.ExerciseRepository
import com.repz.app.core.domain.model.Exercise
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * A hand-written fake (N7): an in-memory store keyed by [Exercise.id], tombstones included, the
 * same shape as [com.repz.app.core.testing.workout.FakeWorkoutRepository].
 * [saveCustomExerciseResult] is returned as-is, without writing, when it is a configured
 * [DomainResult.Failure]; every write/delete/undo/confirm call is recorded for assertions.
 */
class FakeExerciseRepository(
    initial: List<Exercise> = emptyList(),
    var saveCustomExerciseResult: DomainResult<Unit, ValidationError>? = null,
) : ExerciseRepository {
    private data class Entry(
        val exercise: Exercise,
        val isDeleted: Boolean = false,
    )

    private val entries = MutableStateFlow(initial.associate { it.id to Entry(it) })

    val saveCustomExerciseCalls = mutableListOf<Exercise>()
    val deleteCustomExerciseCalls = mutableListOf<ExerciseId>()
    val undoCustomExerciseDeletionCalls = mutableListOf<ExerciseId>()
    val confirmCustomExerciseDeletionCalls = mutableListOf<ExerciseId>()

    override fun observeExercises(): Flow<List<Exercise>> =
        entries.map { current -> current.values.filterNot { it.isDeleted }.map { it.exercise } }

    override fun observeExercise(id: ExerciseId): Flow<Exercise?> =
        entries.map { current -> current[id]?.takeUnless { it.isDeleted }?.exercise }

    override suspend fun findExercisesIncludingDeleted(ids: Set<ExerciseId>): List<Exercise> =
        entries.value.values
            .filter { it.exercise.id in ids }
            .map { it.exercise }

    override suspend fun saveCustomExercise(exercise: Exercise): DomainResult<Unit, ValidationError> {
        saveCustomExerciseCalls += exercise
        val configured = saveCustomExerciseResult
        if (configured is DomainResult.Failure) return configured
        entries.update { it + (exercise.id to Entry(exercise)) }
        return DomainResult.Success(Unit)
    }

    override suspend fun deleteCustomExercise(id: ExerciseId) {
        deleteCustomExerciseCalls += id
        entries.update { current -> current[id]?.let { current + (id to it.copy(isDeleted = true)) } ?: current }
    }

    override suspend fun undoCustomExerciseDeletion(id: ExerciseId) {
        undoCustomExerciseDeletionCalls += id
        entries.update { current -> current[id]?.let { current + (id to it.copy(isDeleted = false)) } ?: current }
    }

    override suspend fun confirmCustomExerciseDeletion(id: ExerciseId) {
        confirmCustomExerciseDeletionCalls += id
    }
}
