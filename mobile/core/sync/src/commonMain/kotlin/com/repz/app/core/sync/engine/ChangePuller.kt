package com.repz.app.core.sync.engine

import androidx.room3.withWriteTransaction
import co.touchlab.kermit.Logger
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.sync.OutboxDao
import com.repz.app.core.database.sync.SyncEntityType
import com.repz.app.core.database.sync.SyncMetaDao
import com.repz.app.core.database.sync.SyncMetaEntity
import com.repz.app.core.domain.sync.RestoreProgress
import com.repz.app.core.network.api.MAX_PULL_LIMIT
import com.repz.app.core.network.api.SyncApi
import com.repz.app.core.network.dto.sync.PullResponseDto
import com.repz.app.core.network.result.NetworkResult
import com.repz.app.core.sync.handler.SyncHandlerRegistry
import com.repz.app.core.sync.record.PersonalRecordsRefresher
import com.repz.app.core.sync.status.SyncStatusTracker
import kotlin.time.Clock
import kotlin.uuid.Uuid

/** Pages `GET /sync/pull` until caught up, applying each page in its own write transaction (ARCHITECTURE §5.5). */
internal class ChangePuller(
    private val database: RepzDatabase,
    private val syncMetaDao: SyncMetaDao,
    private val outboxDao: OutboxDao,
    private val syncApi: SyncApi,
    private val handlers: SyncHandlerRegistry,
    private val recordsRefresher: PersonalRecordsRefresher,
    private val statusTracker: SyncStatusTracker,
    private val clock: Clock,
    private val logger: Logger,
) {
    /**
     * Returns `null` on success (every page applied, `hasMore == false`); a non-null [SyncOutcome]
     * means the whole sync() run must stop here. The three early returns below (stopped, a failed
     * page, the normal end of the loop) are the clearest shape for this "stop here, or keep going"
     * sequence, same as [com.repz.app.core.network.result.apiCall]'s.
     */
    @Suppress("ReturnCount")
    suspend fun pullUntilDone(isStopRequested: () -> Boolean): SyncOutcome? {
        var meta = syncMetaDao.get()
        val restoring = meta.initialPullCompletedAt == null

        while (true) {
            if (isStopRequested()) return SyncOutcome.Stopped

            when (val result = syncApi.pull(meta.pullCursor, MAX_PULL_LIMIT)) {
                is NetworkResult.Failure -> {
                    logger.w { "pullUntilDone: page fetch failed, cursor stays at ${meta.pullCursor}: ${result.error}" }
                    return result.error.toSyncOutcome()
                }

                is NetworkResult.Success -> {
                    val page = result.value
                    meta = applyPage(page, meta, restoring)
                    logger.i { "pullUntilDone: cursor ${meta.pullCursor}, ${page.changes.size} changes, hasMore=${page.hasMore}" }
                    if (!page.hasMore) break
                }
            }
        }

        if (restoring) {
            recordsRefresher.refreshAll()
            syncMetaDao.save(meta.copy(initialPullCompletedAt = clock.now()))
        }
        return null
    }

    private suspend fun applyPage(
        page: PullResponseDto,
        meta: SyncMetaEntity,
        restoring: Boolean,
    ): SyncMetaEntity {
        var touchedExerciseIds = emptySet<Uuid>()

        val newMeta =
            database.withWriteTransaction {
                for ((wireType, changesOfType) in page.changes.groupBy { it.wireType() }) {
                    val handler = handlers.forWireType(wireType)
                    val pendingIds = outboxDao.findPendingEntityIds(handler.entityType, changesOfType.map { it.id }).toSet()
                    for (change in changesOfType) {
                        if (change.id in pendingIds) continue
                        touchedExerciseIds = touchedExerciseIds + handler.applyPulled(change)
                    }
                }

                val updatedMeta =
                    meta.copy(
                        pullCursor = page.nextCursor,
                        restoreTotalChanges = meta.restoreTotalChanges ?: page.totalEstimate.total.takeIf { restoring },
                        restoreTotalWorkouts = meta.restoreTotalWorkouts ?: page.totalEstimate.workout.takeIf { restoring },
                    )
                syncMetaDao.save(updatedMeta)
                updatedMeta
            }

        if (restoring) {
            statusTracker.onRestoreProgress(restoreProgressFor(page, newMeta))
        } else if (touchedExerciseIds.isNotEmpty()) {
            recordsRefresher.refreshForExercises(touchedExerciseIds)
        }
        return newMeta
    }

    private fun restoreProgressFor(
        page: PullResponseDto,
        meta: SyncMetaEntity,
    ): RestoreProgress {
        val workoutChangesInPage = page.changes.count { handlers.forWireType(it.wireType()).entityType == SyncEntityType.WORKOUT }
        val remainingChanges = (page.totalEstimate.total - page.changes.size).coerceAtLeast(0)
        val remainingWorkouts = (page.totalEstimate.workout - workoutChangesInPage).coerceAtLeast(0)
        val totalChanges = meta.restoreTotalChanges ?: 0
        val totalWorkouts = meta.restoreTotalWorkouts ?: 0
        return RestoreProgress(
            restoredWorkouts = (totalWorkouts - remainingWorkouts).coerceIn(0, totalWorkouts),
            totalWorkouts = totalWorkouts,
            restoredChanges = (totalChanges - remainingChanges).coerceIn(0, totalChanges),
            totalChanges = totalChanges,
        )
    }
}
