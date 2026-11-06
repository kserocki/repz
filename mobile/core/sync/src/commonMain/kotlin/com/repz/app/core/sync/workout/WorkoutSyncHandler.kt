package com.repz.app.core.sync.workout

import co.touchlab.kermit.Logger
import com.repz.app.core.database.sync.SyncEntityType
import com.repz.app.core.database.workout.WorkoutDao
import com.repz.app.core.network.dto.SyncTypeDto
import com.repz.app.core.network.dto.sync.AcceptedChangeDto
import com.repz.app.core.network.dto.sync.PushChangeDto
import com.repz.app.core.network.dto.sync.SyncChangeDto
import com.repz.app.core.sync.handler.SyncHandler
import com.repz.app.core.sync.logging.SYNC_LOG_TAG
import kotlin.time.Instant
import kotlin.uuid.Uuid

internal class WorkoutSyncHandler(
    private val workoutDao: WorkoutDao,
    private val logger: Logger = Logger.withTag(SYNC_LOG_TAG),
) : SyncHandler {
    override val entityType = SyncEntityType.WORKOUT
    override val wireType = SyncTypeDto.WORKOUT

    @Suppress("ReturnCount")
    override suspend fun buildUpsert(id: Uuid): PushChangeDto? {
        val aggregate = workoutDao.findWorkoutIncludingDeleted(id) ?: return null
        if (aggregate.workout.finishedAt == null) return null
        val payload =
            aggregate.toUpsertPayload() ?: run {
                logger.e { "buildUpsert: workout $id has an unrecognised set type, dropping it from this run" }
                return null
            }
        return PushChangeDto.UpsertWorkout(id, payload)
    }

    @Suppress("ReturnCount")
    override suspend fun applyAccepted(
        accepted: AcceptedChangeDto,
        now: Instant,
    ): Set<Uuid> {
        if (accepted.version == null) return emptySet()
        if (!accepted.deleted) {
            workoutDao.setServerVersion(accepted.id, accepted.version)
            return emptySet()
        }
        val exerciseIds = existingExerciseIds(accepted.id)
        workoutDao.setDeleted(accepted.id, isDeleted = true, updatedAt = now)
        workoutDao.deleteChildren(accepted.id)
        workoutDao.setServerVersion(accepted.id, accepted.version)
        return exerciseIds
    }

    override suspend fun applyPulled(change: SyncChangeDto): Set<Uuid> =
        when (change) {
            is SyncChangeDto.Workout -> applyUpsert(change)
            is SyncChangeDto.Tombstone -> applyTombstone(change)
            else -> error("WorkoutSyncHandler can't apply $change")
        }

    private suspend fun applyUpsert(change: SyncChangeDto.Workout): Set<Uuid> {
        val before = existingExerciseIds(change.id)
        val (entity, exercises, sets) = change.toEntities()
        workoutDao.saveSyncedWorkout(entity, exercises, sets)
        return before +
            change.payload.exercises
                .map { it.exerciseId }
                .toSet()
    }

    private suspend fun applyTombstone(change: SyncChangeDto.Tombstone): Set<Uuid> {
        val before = existingExerciseIds(change.id)
        workoutDao.setDeleted(change.id, isDeleted = true, updatedAt = change.updatedAt)
        workoutDao.deleteChildren(change.id)
        workoutDao.setServerVersion(change.id, change.version)
        return before
    }

    private suspend fun existingExerciseIds(id: Uuid): Set<Uuid> =
        workoutDao
            .findWorkoutIncludingDeleted(id)
            ?.exercises
            ?.map { it.exercise.exerciseId }
            ?.toSet()
            .orEmpty()
}
