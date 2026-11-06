package com.repz.app.core.sync.exercise

import co.touchlab.kermit.Logger
import com.repz.app.core.database.exercise.ExerciseDao
import com.repz.app.core.database.sync.SyncEntityType
import com.repz.app.core.network.dto.SyncTypeDto
import com.repz.app.core.network.dto.sync.AcceptedChangeDto
import com.repz.app.core.network.dto.sync.PushChangeDto
import com.repz.app.core.network.dto.sync.SyncChangeDto
import com.repz.app.core.sync.handler.SyncHandler
import com.repz.app.core.sync.logging.SYNC_LOG_TAG
import kotlin.time.Instant
import kotlin.uuid.Uuid

internal class ExerciseSyncHandler(
    private val exerciseDao: ExerciseDao,
    private val logger: Logger = Logger.withTag(SYNC_LOG_TAG),
) : SyncHandler {
    override val entityType = SyncEntityType.EXERCISE
    override val wireType = SyncTypeDto.EXERCISE

    /** Only a custom exercise is ever pushed; a catalog row should never be enqueued. */
    @Suppress("ReturnCount")
    override suspend fun buildUpsert(id: Uuid): PushChangeDto? {
        val exercise = exerciseDao.findExercisesIncludingDeleted(listOf(id)).firstOrNull()?.exercise ?: return null
        if (!exercise.isCustom) return null
        val payload =
            exercise.toCustomUpsertPayload() ?: run {
                logger.e { "buildUpsert: exercise $id has an unrecognised muscle or equipment value, dropping it from this run" }
                return null
            }
        return PushChangeDto.UpsertCustomExercise(id, payload)
    }

    override suspend fun applyAccepted(
        accepted: AcceptedChangeDto,
        now: Instant,
    ): Set<Uuid> {
        if (accepted.version == null) return emptySet()
        if (accepted.deleted) {
            exerciseDao.setDeleted(accepted.id, isDeleted = true, updatedAt = now)
        }
        exerciseDao.setServerVersion(accepted.id, accepted.version)
        return emptySet()
    }

    /** A tombstone keeps the row and its translations (ADR-0023): history still shows the exercise's name. */
    override suspend fun applyPulled(change: SyncChangeDto): Set<Uuid> {
        when (change) {
            is SyncChangeDto.Exercise -> {
                val (entity, translations) = change.toEntities()
                exerciseDao.saveSyncedExercise(entity, translations)
            }

            is SyncChangeDto.Tombstone -> {
                exerciseDao.setDeleted(change.id, isDeleted = true, updatedAt = change.updatedAt)
                exerciseDao.setServerVersion(change.id, change.version)
            }

            else -> {
                error("ExerciseSyncHandler can't apply $change")
            }
        }
        return emptySet()
    }
}
