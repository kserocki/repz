package com.repz.app.core.database.sync

import androidx.room3.Room
import androidx.room3.withWriteTransaction
import app.cash.turbine.test
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.buildRepzDatabase
import com.repz.app.core.database.testUuid
import com.repz.app.core.database.workout.WorkoutEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class OutboxDaoTest {
    // Room needs a real dispatcher for its query/invalidation work, not the runTest scheduler.
    private val database =
        Room.inMemoryDatabaseBuilder<RepzDatabase>().buildRepzDatabase(Dispatchers.IO)
    private val outboxDao = database.outboxDao()
    private val entityId = testUuid("1")
    private val now = Instant.parse("2024-01-01T00:00:00Z")

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun givenTenSuccessiveEnqueues_whenCoalesced_thenOneRowRemainsWithRevisionTen() =
        runTest {
            repeat(10) {
                outboxDao.enqueue(SyncEntityType.WORKOUT, entityId, OutboxOperation.UPSERT, now + it.seconds)
            }

            val entries = outboxDao.findReadyToPush(now, limit = 100)
            assertEquals(1, entries.size)
            assertEquals(10, entries.single().revision)
        }

    @Test
    fun givenAHeldDeleteEntry_whenFindingReadyToPush_thenExcludedBeforeHeldUntilIncludedAtOrAfterItAndIncludedAfterReleaseHold() =
        runTest {
            val heldUntil = now + 600.seconds
            outboxDao.enqueue(SyncEntityType.WORKOUT, entityId, OutboxOperation.DELETE, now, heldUntil)

            assertTrue(outboxDao.findReadyToPush(heldUntil - 1.seconds, 100).isEmpty())
            assertEquals(1, outboxDao.findReadyToPush(heldUntil, 100).size)

            outboxDao.enqueue(SyncEntityType.WORKOUT, entityId, OutboxOperation.DELETE, now, heldUntil + 600.seconds)
            assertTrue(outboxDao.findReadyToPush(heldUntil, 100).isEmpty())

            outboxDao.releaseHold(SyncEntityType.WORKOUT, entityId)
            assertEquals(1, outboxDao.findReadyToPush(now, 100).size)
        }

    @Test
    fun givenSeveralEntries_whenFindingReadyToPush_thenOrderedByCreatedAtThenTypeThenIdWithLimitRespected() =
        runTest {
            val lastCreated = testUuid("2")
            val sameInstantLowerId = testUuid("3")
            val sameInstantHigherId = testUuid("4")
            outboxDao.enqueue(SyncEntityType.WORKOUT, lastCreated, OutboxOperation.UPSERT, now + 60.seconds)
            outboxDao.enqueue(SyncEntityType.WORKOUT, sameInstantHigherId, OutboxOperation.UPSERT, now)
            outboxDao.enqueue(SyncEntityType.WORKOUT, sameInstantLowerId, OutboxOperation.UPSERT, now)

            val entries = outboxDao.findReadyToPush(now + 600.seconds, limit = 2)

            assertEquals(listOf(sameInstantLowerId, sameInstantHigherId), entries.map { it.entityId })
        }

    @Test
    fun givenAnEntryAlreadyRead_whenDeletedWithAStaleRevision_thenTheRowSurvivesAndZeroRowsAreReported() =
        runTest {
            outboxDao.enqueue(SyncEntityType.WORKOUT, entityId, OutboxOperation.UPSERT, now)
            val staleRevision = outboxDao.findReadyToPush(now, 100).single().revision
            outboxDao.enqueue(SyncEntityType.WORKOUT, entityId, OutboxOperation.UPSERT, now + 60.seconds)

            val deletedRows = outboxDao.deleteIfUnchanged(SyncEntityType.WORKOUT, entityId, staleRevision)

            assertEquals(0, deletedRows)
            assertEquals(1, outboxDao.findReadyToPush(now, 100).size)
        }

    @Test
    fun givenAHeldEntry_whenFindingPendingEntityIds_thenItIsIncluded() =
        runTest {
            outboxDao.enqueue(SyncEntityType.WORKOUT, entityId, OutboxOperation.DELETE, now, now + 600.seconds)

            val pending = outboxDao.findPendingEntityIds(SyncEntityType.WORKOUT, listOf(entityId))

            assertEquals(listOf(entityId), pending)
        }

    @Test
    fun givenOutboxEntries_whenObservingPendingCount_thenEmitsEachChange() =
        runTest {
            outboxDao.observePendingCount().test {
                assertEquals(0, awaitItem())
                outboxDao.enqueue(SyncEntityType.WORKOUT, entityId, OutboxOperation.UPSERT, now)
                assertEquals(1, awaitItem())
            }
        }

    @Test
    fun givenAFailingWriteTransaction_whenSavingAWorkoutAndEnqueuing_thenNeitherThePersistedWorkoutNorTheOutboxRowSurvive() =
        runTest {
            val workoutId = testUuid("5")
            val workout =
                WorkoutEntity(
                    workoutId,
                    null,
                    "Push day",
                    "",
                    now,
                    now,
                    now,
                    serverVersion = null,
                    isDeleted = false,
                )

            assertFailsWith<RollbackTestException> {
                database.withWriteTransaction {
                    database.workoutDao().saveSyncedWorkout(workout, emptyList(), emptyList())
                    outboxDao.enqueue(SyncEntityType.WORKOUT, workoutId, OutboxOperation.UPSERT, now)
                    throw RollbackTestException()
                }
            }

            assertNull(database.workoutDao().findWorkoutIncludingDeleted(workoutId))
            assertTrue(outboxDao.findPendingEntityIds(SyncEntityType.WORKOUT, listOf(workoutId)).isEmpty())
        }

    private class RollbackTestException : Exception()
}
