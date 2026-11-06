package com.repz.app.core.sync.handler

import com.repz.app.core.database.sync.SyncEntityType
import com.repz.app.core.network.dto.SyncTypeDto
import com.repz.app.core.network.dto.sync.AcceptedChangeDto
import com.repz.app.core.network.dto.sync.PushChangeDto
import com.repz.app.core.network.dto.sync.SyncChangeDto
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * One syncable entity's push/pull behaviour (ARCHITECTURE §5.3). Every method runs inside the
 * engine's own write transaction: a handler never opens one of its own.
 */
interface SyncHandler {
    /** The Room-side key (`outbox.entity_type`, local only). */
    val entityType: SyncEntityType

    /** The wire-side key (`PushChangeDto`/`SyncChangeDto`'s `type`). */
    val wireType: SyncTypeDto

    /**
     * An upsert built from the current local row, or `null` when nothing pushable exists for [id]
     * (the row is missing, or - for a workout - it is still active).
     */
    suspend fun buildUpsert(id: Uuid): PushChangeDto?

    /** Applies an accepted push response; returns the exercise ids whose personal records may have changed. */
    suspend fun applyAccepted(
        accepted: AcceptedChangeDto,
        now: Instant,
    ): Set<Uuid>

    /** Applies one pulled change (upsert or tombstone); never called for an id with a pending outbox row. */
    suspend fun applyPulled(change: SyncChangeDto): Set<Uuid>
}
