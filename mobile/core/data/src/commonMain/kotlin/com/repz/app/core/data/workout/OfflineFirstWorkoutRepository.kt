package com.repz.app.core.data.workout

import androidx.room3.withWriteTransaction
import co.touchlab.kermit.Logger
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.sync.OutboxDao
import com.repz.app.core.database.sync.OutboxOperation
import com.repz.app.core.database.sync.SyncEntityType
import com.repz.app.core.database.workout.WorkoutDao
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.Workout
import com.repz.app.core.domain.model.WorkoutId
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import com.repz.app.core.domain.sync.DELETE_UNDO_WINDOW
import com.repz.app.core.domain.sync.SyncScheduler
import com.repz.app.core.domain.validation.validateFinishedWorkout
import com.repz.app.core.domain.workout.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single
import kotlin.time.Clock

internal const val WORKOUT_LOG_TAG = "Workout"

/**
 * Rule 6 and ADR-0023: every read goes through Room, every synced write is its entity plus an
 * outbox row in one transaction, and [syncScheduler] is asked for a sync only after that
 * transaction commits (R7). [saveFinishedWorkout]/[deleteWorkout]/[undoWorkoutDeletion] are called
 * through their use cases ([com.repz.app.core.domain.workout.SaveFinishedWorkoutUseCase] etc.),
 * which also keep personal records in sync; later features call through those, not this class
 * directly.
 */
@Single
class OfflineFirstWorkoutRepository(
    private val database: RepzDatabase,
    private val workoutDao: WorkoutDao,
    private val outboxDao: OutboxDao,
    private val syncScheduler: SyncScheduler,
    private val clock: Clock,
    private val logger: Logger = Logger.withTag(WORKOUT_LOG_TAG),
) : WorkoutRepository {
    override fun observeActiveWorkout(): Flow<Workout?> = workoutDao.observeActiveWorkout().map { it?.toDomainOrNull(logger) }

    override fun observeFinishedWorkouts(): Flow<List<Workout>> = workoutDao.observeFinishedWorkouts().map { it.toDomainList(logger) }

    override fun observeWorkout(id: WorkoutId): Flow<Workout?> = workoutDao.observeWorkout(id.value).map { it?.toDomainOrNull(logger) }

    override suspend fun findWorkoutIncludingDeleted(id: WorkoutId): Workout? =
        workoutDao.findWorkoutIncludingDeleted(id.value)?.toDomainOrNull(logger)

    override suspend fun findFinishedWorkouts(): List<Workout> = workoutDao.findFinishedWorkouts().toDomainList(logger)

    override suspend fun findFinishedWorkoutsContaining(exerciseIds: Set<ExerciseId>): List<Workout> =
        workoutDao.findFinishedWorkoutsContainingExercises(exerciseIds.map { it.value }).toDomainList(logger)

    override suspend fun saveActiveWorkout(workout: Workout) {
        require(workout.finishedAt == null) { "saveActiveWorkout requires finishedAt == null: ${workout.id}" }
        val now = clock.now()
        workoutDao.saveLocalWorkout(workout.toEntity(now), workout.toExerciseEntities(), workout.toSetEntities())
    }

    override suspend fun discardActiveWorkout(id: WorkoutId) {
        val stored = workoutDao.findWorkoutIncludingDeleted(id.value)
        if (stored != null && stored.workout.finishedAt == null) {
            workoutDao.purge(id.value)
        }
    }

    override suspend fun saveFinishedWorkout(workout: Workout): DomainResult<Unit, ValidationError> {
        require(workout.finishedAt != null) { "saveFinishedWorkout requires finishedAt != null: ${workout.id}" }
        return when (val validation = validateFinishedWorkout(workout)) {
            is DomainResult.Success -> {
                val now = clock.now()
                database.withWriteTransaction {
                    workoutDao.saveLocalWorkout(workout.toEntity(now), workout.toExerciseEntities(), workout.toSetEntities())
                    outboxDao.enqueue(SyncEntityType.WORKOUT, workout.id.value, OutboxOperation.UPSERT, now)
                }
                syncScheduler.requestSync()
                DomainResult.Success(Unit)
            }

            is DomainResult.Failure -> {
                validation
            }
        }
    }

    override suspend fun deleteWorkout(id: WorkoutId) {
        val now = clock.now()
        database.withWriteTransaction {
            workoutDao.setDeleted(id.value, isDeleted = true, updatedAt = now)
            outboxDao.enqueue(SyncEntityType.WORKOUT, id.value, OutboxOperation.DELETE, now, heldUntil = now + DELETE_UNDO_WINDOW)
        }
    }

    override suspend fun undoWorkoutDeletion(id: WorkoutId) {
        val now = clock.now()
        database.withWriteTransaction {
            workoutDao.setDeleted(id.value, isDeleted = false, updatedAt = now)
            outboxDao.enqueue(SyncEntityType.WORKOUT, id.value, OutboxOperation.UPSERT, now)
        }
        syncScheduler.requestSync()
    }

    override suspend fun confirmWorkoutDeletion(id: WorkoutId) {
        outboxDao.releaseHold(SyncEntityType.WORKOUT, id.value)
        syncScheduler.requestSync()
    }
}
