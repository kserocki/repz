package com.repz.app.core.database.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlin.uuid.Uuid

class OutboxCoalescingTest {
    private val entityId = Uuid.parse("0190f3e0-0000-7000-8000-000000000001")
    private val createdAt = Instant.parse("2024-01-01T00:00:00Z")
    private val coalescedAt = Instant.parse("2024-01-01T00:05:00Z")
    private val heldUntil = Instant.parse("2024-01-01T00:10:00Z")
    private val refreshedHeldUntil = Instant.parse("2024-01-01T00:15:00Z")

    @Test
    fun givenNoPendingRow_whenUpsertCoalesced_thenCreatesAnUnheldUpsertRow() {
        val entry =
            coalesceOutboxEntry(existing = null, SyncEntityType.WORKOUT, entityId, OutboxOperation.UPSERT, createdAt, heldUntil = null)

        assertEquals(
            OutboxEntryEntity(
                SyncEntityType.WORKOUT,
                entityId,
                OutboxOperation.UPSERT,
                createdAt,
                createdAt,
                revision = 1,
                attempts = 0,
                heldUntil = null,
            ),
            entry,
        )
    }

    @Test
    fun givenNoPendingRow_whenDeleteCoalesced_thenCreatesADeleteRowWithTheGivenHold() {
        val entry = coalesceOutboxEntry(existing = null, SyncEntityType.WORKOUT, entityId, OutboxOperation.DELETE, createdAt, heldUntil)

        assertEquals(
            OutboxEntryEntity(
                SyncEntityType.WORKOUT,
                entityId,
                OutboxOperation.DELETE,
                createdAt,
                createdAt,
                revision = 1,
                attempts = 0,
                heldUntil,
            ),
            entry,
        )
    }

    @Test
    fun givenPendingUpsert_whenUpsertCoalesced_thenStaysUpsert() {
        val pending = pendingUpsert()

        val entry = coalesceOutboxEntry(pending, SyncEntityType.WORKOUT, entityId, OutboxOperation.UPSERT, coalescedAt, heldUntil = null)

        assertEquals(OutboxOperation.UPSERT, entry.operation)
        assertNull(entry.heldUntil)
    }

    @Test
    fun givenPendingUpsert_whenDeleteCoalesced_thenBecomesDeleteWithTheHoldSet() {
        val pending = pendingUpsert()

        val entry = coalesceOutboxEntry(pending, SyncEntityType.WORKOUT, entityId, OutboxOperation.DELETE, coalescedAt, heldUntil)

        assertEquals(OutboxOperation.DELETE, entry.operation)
        assertEquals(heldUntil, entry.heldUntil)
    }

    @Test
    fun givenPendingDelete_whenUpsertCoalesced_thenBecomesUpsertAndTheHoldIsCleared() {
        val pending = pendingDelete(heldUntil)

        val entry = coalesceOutboxEntry(pending, SyncEntityType.WORKOUT, entityId, OutboxOperation.UPSERT, coalescedAt, heldUntil = null)

        assertEquals(OutboxOperation.UPSERT, entry.operation)
        assertNull(entry.heldUntil)
    }

    @Test
    fun givenPendingDelete_whenDeleteCoalesced_thenStaysDeleteAndTheHoldIsRefreshed() {
        val pending = pendingDelete(heldUntil)

        val entry = coalesceOutboxEntry(pending, SyncEntityType.WORKOUT, entityId, OutboxOperation.DELETE, coalescedAt, refreshedHeldUntil)

        assertEquals(OutboxOperation.DELETE, entry.operation)
        assertEquals(refreshedHeldUntil, entry.heldUntil)
    }

    @Test
    fun givenPendingRow_whenCoalesced_thenCreatedAtIsKeptAndRevisionIncrementsAndAttemptsResets() {
        val pending = pendingUpsert().copy(attempts = 3)

        val entry = coalesceOutboxEntry(pending, SyncEntityType.WORKOUT, entityId, OutboxOperation.UPSERT, coalescedAt, heldUntil = null)

        assertEquals(createdAt, entry.createdAt)
        assertEquals(coalescedAt, entry.updatedAt)
        assertEquals(pending.revision + 1, entry.revision)
        assertEquals(0, entry.attempts)
    }

    @Test
    fun givenTenSuccessiveUpserts_whenCoalescedInOrder_thenRevisionCountsEachOne() {
        var entry: OutboxEntryEntity? = null

        repeat(10) {
            entry = coalesceOutboxEntry(entry, SyncEntityType.WORKOUT, entityId, OutboxOperation.UPSERT, coalescedAt, heldUntil = null)
        }

        assertEquals(10, entry?.revision)
    }

    @Test
    fun givenUpsertOperation_whenHeldUntilIsGiven_thenFailsAsAProgrammerError() {
        assertFailsWith<IllegalArgumentException> {
            coalesceOutboxEntry(existing = null, SyncEntityType.WORKOUT, entityId, OutboxOperation.UPSERT, createdAt, heldUntil)
        }
    }

    private fun pendingUpsert(): OutboxEntryEntity =
        OutboxEntryEntity(
            SyncEntityType.WORKOUT,
            entityId,
            OutboxOperation.UPSERT,
            createdAt,
            createdAt,
            revision = 1,
            attempts = 0,
            heldUntil = null,
        )

    private fun pendingDelete(heldUntil: Instant): OutboxEntryEntity =
        OutboxEntryEntity(
            SyncEntityType.WORKOUT,
            entityId,
            OutboxOperation.DELETE,
            createdAt,
            createdAt,
            revision = 1,
            attempts = 0,
            heldUntil,
        )
}
