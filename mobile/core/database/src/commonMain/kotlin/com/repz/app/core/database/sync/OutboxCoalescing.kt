package com.repz.app.core.database.sync

import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Folds a new outbox operation into the entity's existing pending row, if any: `created_at` is
 * kept (so a frequently edited entity never starves behind newer ones), `updated_at` and
 * `revision` always advance, and `attempts` resets. A hold ([heldUntil]) only ever applies to a
 * resulting [OutboxOperation.DELETE]; an [OutboxOperation.UPSERT] always clears it, which is how an
 * undo (delete, then upsert) releases the hold.
 */
internal fun coalesceOutboxEntry(
    existing: OutboxEntryEntity?,
    entityType: SyncEntityType,
    entityId: Uuid,
    operation: OutboxOperation,
    now: Instant,
    heldUntil: Instant?,
): OutboxEntryEntity {
    require(operation != OutboxOperation.UPSERT || heldUntil == null) {
        "An upsert can't be held: heldUntil must be null, was $heldUntil"
    }

    return OutboxEntryEntity(
        entityType = entityType,
        entityId = entityId,
        operation = operation,
        createdAt = existing?.createdAt ?: now,
        updatedAt = now,
        revision = (existing?.revision ?: 0L) + 1L,
        attempts = 0,
        heldUntil =
            when (operation) {
                OutboxOperation.UPSERT -> null
                OutboxOperation.DELETE -> heldUntil
            },
    )
}
