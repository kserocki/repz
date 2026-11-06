package com.repz.app.core.data.exercise

import androidx.room3.withWriteTransaction
import co.touchlab.kermit.Logger
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.exercise.ExerciseDao
import com.repz.app.core.database.sync.OutboxDao
import com.repz.app.core.database.sync.OutboxOperation
import com.repz.app.core.database.sync.SyncEntityType
import com.repz.app.core.domain.exercise.ExerciseRepository
import com.repz.app.core.domain.model.Exercise
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.ExerciseOrigin
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import com.repz.app.core.domain.sync.DELETE_UNDO_WINDOW
import com.repz.app.core.domain.sync.SyncScheduler
import com.repz.app.core.domain.validation.validateCustomExercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single
import kotlin.time.Clock

internal const val EXERCISE_LOG_TAG = "Exercise"

/** Rule 6 and ADR-0020/0023: catalog exercises are read-only; only a custom exercise's save/delete/undo touches the outbox. */
@Single
class OfflineFirstExerciseRepository(
    private val database: RepzDatabase,
    private val exerciseDao: ExerciseDao,
    private val outboxDao: OutboxDao,
    private val syncScheduler: SyncScheduler,
    private val clock: Clock,
    private val logger: Logger = Logger.withTag(EXERCISE_LOG_TAG),
) : ExerciseRepository {
    override fun observeExercises(): Flow<List<Exercise>> = exerciseDao.observeExercises().map { it.toDomainList(logger) }

    override fun observeExercise(id: ExerciseId): Flow<Exercise?> = exerciseDao.observeExercise(id.value).map { it?.toDomainOrNull(logger) }

    override suspend fun findExercisesIncludingDeleted(ids: Set<ExerciseId>): List<Exercise> =
        exerciseDao.findExercisesIncludingDeleted(ids.map { it.value }).toDomainList(logger)

    override suspend fun saveCustomExercise(exercise: Exercise): DomainResult<Unit, ValidationError> {
        require(exercise.origin is ExerciseOrigin.Custom) { "saveCustomExercise requires origin Custom: ${exercise.id}" }
        return when (val validation = validateCustomExercise(exercise)) {
            is DomainResult.Success -> {
                val now = clock.now()
                database.withWriteTransaction {
                    exerciseDao.saveLocalExercise(exercise.toEntity(now), exercise.toTranslationEntities())
                    outboxDao.enqueue(SyncEntityType.EXERCISE, exercise.id.value, OutboxOperation.UPSERT, now)
                }
                syncScheduler.requestSync()
                DomainResult.Success(Unit)
            }

            is DomainResult.Failure -> {
                validation
            }
        }
    }

    override suspend fun deleteCustomExercise(id: ExerciseId) {
        val stored = exerciseDao.findExercisesIncludingDeleted(listOf(id.value)).firstOrNull()
        if (stored == null || !stored.exercise.isCustom) {
            logger.w { "deleteCustomExercise is a no-op for a catalog or unknown id: ${id.value}" }
            return
        }
        val now = clock.now()
        database.withWriteTransaction {
            exerciseDao.setDeleted(id.value, isDeleted = true, updatedAt = now)
            outboxDao.enqueue(SyncEntityType.EXERCISE, id.value, OutboxOperation.DELETE, now, heldUntil = now + DELETE_UNDO_WINDOW)
        }
    }

    override suspend fun undoCustomExerciseDeletion(id: ExerciseId) {
        val now = clock.now()
        database.withWriteTransaction {
            exerciseDao.setDeleted(id.value, isDeleted = false, updatedAt = now)
            outboxDao.enqueue(SyncEntityType.EXERCISE, id.value, OutboxOperation.UPSERT, now)
        }
        syncScheduler.requestSync()
    }

    override suspend fun confirmCustomExerciseDeletion(id: ExerciseId) {
        outboxDao.releaseHold(SyncEntityType.EXERCISE, id.value)
        syncScheduler.requestSync()
    }
}
