package com.repz.app.core.sync.engine

import androidx.room3.withWriteTransaction
import co.touchlab.kermit.Logger
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.sync.OutboxDao
import com.repz.app.core.database.sync.OutboxEntryEntity
import com.repz.app.core.database.sync.OutboxOperation
import com.repz.app.core.database.sync.SyncEntityType
import com.repz.app.core.database.sync.SyncRejectionDao
import com.repz.app.core.database.sync.SyncRejectionEntity
import com.repz.app.core.network.api.SyncApi
import com.repz.app.core.network.dto.sync.AcceptedChangeDto
import com.repz.app.core.network.dto.sync.PushChangeDto
import com.repz.app.core.network.dto.sync.PushRequestDto
import com.repz.app.core.network.dto.sync.PushResponseDto
import com.repz.app.core.network.dto.sync.RejectedChangeDto
import com.repz.app.core.network.dto.sync.encodedSizeInBytes
import com.repz.app.core.network.result.NetworkResult
import com.repz.app.core.sync.handler.SyncHandlerRegistry
import com.repz.app.core.sync.record.PersonalRecordsRefresher
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

private data class OutboxKey(
    val entityType: SyncEntityType,
    val entityId: Uuid,
)

private fun List<AcceptedChangeDto>.acceptedCountsByType() = groupingBy { it.type }.eachCount()

private fun List<RejectedChangeDto>.rejectedCountsByType() = groupingBy { it.type }.eachCount()

/**
 * Pushes every ready outbox row, batched by [batchPushChanges] (ARCHITECTURE §5.3/§5.4, ADR-0020).
 *
 * A null from [com.repz.app.core.sync.handler.SyncHandler.buildUpsert] (the row is missing, or -
 * for a workout - it is still active) always drops the outbox row via [OutboxDao.deleteIfUnchanged]:
 * both reasons are defensive only (an active workout never actually reaches the outbox,
 * ARCHITECTURE §5.4), and dropping is harmless either way - a later legitimate write re-enqueues
 * its own outbox row. The revision guard still protects a row edited after [pushReadyChanges] read
 * it: `deleteIfUnchanged` only removes the row if its revision still matches that earlier read, so a
 * row re-enqueued in the meantime survives.
 */
internal class OutboxPusher(
    private val database: RepzDatabase,
    private val outboxDao: OutboxDao,
    private val syncRejectionDao: SyncRejectionDao,
    private val syncApi: SyncApi,
    private val handlers: SyncHandlerRegistry,
    private val recordsRefresher: PersonalRecordsRefresher,
    private val clock: Clock,
    private val logger: Logger,
) {
    /**
     * Returns `null` on success (every ready row was attempted this run); a non-null [SyncOutcome]
     * means the whole sync() run must stop here. The early returns below are the clearest shape for
     * this "stop here, or keep going" sequence, same as [com.repz.app.core.network.result.apiCall]'s.
     */
    @Suppress("ReturnCount")
    suspend fun pushReadyChanges(isStopRequested: () -> Boolean): SyncOutcome? {
        val attempted = mutableSetOf<OutboxKey>()

        while (true) {
            if (isStopRequested()) return SyncOutcome.Stopped

            val entries = outboxDao.findReadyToPush(clock.now(), limit = MAX_PUSH_CHANGES + attempted.size)
            val entriesByKey = entries.associateBy { it.key() }
            val unattempted = entries.filterNot { it.key() in attempted }
            if (unattempted.isEmpty()) return null

            val changes = mutableListOf<PushChangeDto>()
            for (entry in unattempted) {
                val change = buildChange(entry)
                if (change == null) {
                    attempted += entry.key()
                } else {
                    changes += change
                }
            }

            for (batch in batchPushChanges(changes)) {
                pushBatch(batch, entriesByKey, attempted)?.let { return it }
            }
        }
    }

    private suspend fun buildChange(entry: OutboxEntryEntity): PushChangeDto? {
        val handler = handlers.forEntityType(entry.entityType)
        return when (entry.operation) {
            OutboxOperation.DELETE -> {
                PushChangeDto.Delete(handler.wireType, entry.entityId)
            }

            OutboxOperation.UPSERT -> {
                val change = handler.buildUpsert(entry.entityId)
                if (change == null) {
                    logger.w {
                        "buildChange: nothing to push for ${entry.entityType}/${entry.entityId}, dropping its outbox row if unchanged"
                    }
                    outboxDao.deleteIfUnchanged(entry.entityType, entry.entityId, entry.revision)
                }
                change
            }
        }
    }

    @Suppress("ReturnCount")
    private suspend fun pushBatch(
        batch: List<PushChangeDto>,
        entriesByKey: Map<OutboxKey, OutboxEntryEntity>,
        attempted: MutableSet<OutboxKey>,
    ): SyncOutcome? {
        val requestBytes = batch.sumOf { it.encodedSizeInBytes() }
        logger.i { "pushBatch: sending ${batch.size} changes, $requestBytes bytes" }

        when (val result = syncApi.push(PushRequestDto(batch))) {
            is NetworkResult.Success -> {
                val response = result.value
                logger.i {
                    "pushBatch: response accepted=${response.accepted.acceptedCountsByType()} " +
                        "rejected=${response.rejected.rejectedCountsByType()}"
                }
                applyResponse(response, entriesByKey)
                batch.forEach { attempted += it.key() }
                return null
            }

            is NetworkResult.Failure -> {
                val error = result.error
                if (error.isPayloadTooLarge()) return handleTooLarge(batch, entriesByKey, attempted)

                val outcome = error.toSyncOutcome()
                if (outcome is SyncOutcome.Failed) {
                    batch.forEach { incrementAttempts(it.key()) }
                    logger.w { "pushBatch: failed as a whole with $error, bumped attempts on ${batch.size} rows" }
                }
                return outcome
            }
        }
    }

    @Suppress("ReturnCount")
    private suspend fun handleTooLarge(
        batch: List<PushChangeDto>,
        entriesByKey: Map<OutboxKey, OutboxEntryEntity>,
        attempted: MutableSet<OutboxKey>,
    ): SyncOutcome? {
        if (batch.size == 1) {
            val key = batch.single().key()
            logger.w { "pushBatch: ${key.entityType}/${key.entityId} is too large to push (413), leaving it for a later run" }
            incrementAttempts(key)
            attempted += key
            return null
        }
        val mid = batch.size / 2
        pushBatch(batch.take(mid), entriesByKey, attempted)?.let { return it }
        return pushBatch(batch.drop(mid), entriesByKey, attempted)
    }

    private suspend fun applyResponse(
        response: PushResponseDto,
        entriesByKey: Map<OutboxKey, OutboxEntryEntity>,
    ) {
        val touchedExerciseIds = mutableSetOf<Uuid>()
        val now = clock.now()

        database.withWriteTransaction {
            response.accepted.forEach { accepted ->
                touchedExerciseIds += applyAccepted(accepted, now, entriesByKey)
            }
            response.rejected.forEach { rejected -> applyRejected(rejected, now, entriesByKey) }
        }

        if (touchedExerciseIds.isNotEmpty()) {
            recordsRefresher.refreshForExercises(touchedExerciseIds)
        }
    }

    private suspend fun applyAccepted(
        accepted: AcceptedChangeDto,
        now: Instant,
        entriesByKey: Map<OutboxKey, OutboxEntryEntity>,
    ): Set<Uuid> {
        val handler = handlers.forWireType(accepted.type)
        val touchedExerciseIds = handler.applyAccepted(accepted, now)
        entriesByKey[OutboxKey(handler.entityType, accepted.id)]?.let { entry ->
            outboxDao.deleteIfUnchanged(entry.entityType, entry.entityId, entry.revision)
        }
        syncRejectionDao.delete(handler.entityType, accepted.id)
        return touchedExerciseIds
    }

    private suspend fun applyRejected(
        rejected: RejectedChangeDto,
        now: Instant,
        entriesByKey: Map<OutboxKey, OutboxEntryEntity>,
    ) {
        val handler = handlers.forWireType(rejected.type)
        syncRejectionDao.upsert(
            SyncRejectionEntity(
                entityType = handler.entityType,
                entityId = rejected.id,
                problemType = rejected.problem.type,
                rejectedAt = now,
            ),
        )
        entriesByKey[OutboxKey(handler.entityType, rejected.id)]?.let { entry ->
            outboxDao.deleteIfUnchanged(entry.entityType, entry.entityId, entry.revision)
        }
    }

    private suspend fun incrementAttempts(key: OutboxKey) {
        outboxDao.incrementAttempts(key.entityType, key.entityId)
    }

    private fun OutboxEntryEntity.key() = OutboxKey(entityType, entityId)

    private fun PushChangeDto.key(): OutboxKey {
        val handler = handlers.forWireType(wireType())
        return OutboxKey(handler.entityType, id)
    }
}
