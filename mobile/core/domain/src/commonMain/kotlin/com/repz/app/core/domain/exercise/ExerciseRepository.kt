package com.repz.app.core.domain.exercise

import com.repz.app.core.domain.model.Exercise
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import kotlinx.coroutines.flow.Flow

/** Exercises: the read-only catalog, plus the user's own custom exercises (rule 6: the UI reads only from Room). */
interface ExerciseRepository {
    /** Not deleted; order unspecified, callers sort by localized name. */
    fun observeExercises(): Flow<List<Exercise>>

    fun observeExercise(id: ExerciseId): Flow<Exercise?>

    /** Includes tombstones, so history can still name a deleted exercise (ADR-0023). */
    suspend fun findExercisesIncludingDeleted(ids: Set<ExerciseId>): List<Exercise>

    /** Requires `exercise.origin` to be `ExerciseOrigin.Custom`; catalog exercises are read-only (ADR-0020). */
    suspend fun saveCustomExercise(exercise: Exercise): DomainResult<Unit, ValidationError>

    /** No-op, with a warning log, for a catalog or unknown id. */
    suspend fun deleteCustomExercise(id: ExerciseId)

    suspend fun undoCustomExerciseDeletion(id: ExerciseId)

    suspend fun confirmCustomExerciseDeletion(id: ExerciseId)
}
