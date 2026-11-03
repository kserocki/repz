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
import kotlin.uuid.Uuid

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
    fun givenEntriesCreatedAtDifferentTimes_whenFindingReadyToPush_thenOrderedByCreatedAtFirst() =
        runTest {
            val createdLast = testUuid("2")
            val createdFirst = testUuid("3")
            outboxDao.enqueue(SyncEntityType.WORKOUT, createdLast, OutboxOperation.UPSERT, now + 60.seconds)
            outboxDao.enqueue(SyncEntityType.WORKOUT, createdFirst, OutboxOperation.UPSERT, now)

            val entries = outboxDao.findReadyToPush(now + 600.seconds, limit = 100)

            assertEquals(listOf(createdFirst, createdLast), entries.map { it.entityId })
        }

    @Test
    fun givenEntriesWithTheSameCreatedAtButDifferentTypes_whenFindingReadyToPush_thenTypeBreaksTheTieBeforeId() =
        runTest {
            // entityId deliberately contradicts entityType order: if id were the tie-break instead
            // of type, "exercise" (higher id) would sort after "workout" (lower id), not before it.
            val higherIdLowerType = testUuid("9")
            val lowerIdHigherType = testUuid("1")
            outboxDao.enqueue(SyncEntityType.WORKOUT, lowerIdHigherType, OutboxOperation.UPSERT, now)
            outboxDao.enqueue(SyncEntityType.EXERCISE, higherIdLowerType, OutboxOperation.UPSERT, now)

            val entries = outboxDao.findReadyToPush(now, limit = 100)

            assertEquals(
                listOf(SyncEntityType.EXERCISE to higherIdLowerType, SyncEntityType.WORKOUT to lowerIdHigherType),
                entries.map { it.entityType to it.entityId },
            )
        }

    @Test
    fun givenMoreReadyEntriesThanTheLimit_whenFindingReadyToPush_thenOnlyTheOldestUpToTheLimitAreReturned() =
        runTest {
            val oldest = testUuid("2")
            val secondOldest = testUuid("3")
            outboxDao.enqueue(SyncEntityType.WORKOUT, testUuid("4"), OutboxOperation.UPSERT, now + 120.seconds)
            outboxDao.enqueue(SyncEntityType.WORKOUT, secondOldest, OutboxOperation.UPSERT, now + 60.seconds)
            outboxDao.enqueue(SyncEntityType.WORKOUT, oldest, OutboxOperation.UPSERT, now)

            val entries = outboxDao.findReadyToPush(now + 600.seconds, limit = 2)

            assertEquals(listOf(oldest, secondOldest), entries.map { it.entityId })
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
    fun givenOutboxEntries_whenCountingPending_thenReturnsTheCurrentRowCount() =
        runTest {
            assertEquals(0, outboxDao.countPending())

            outboxDao.enqueue(SyncEntityType.WORKOUT, entityId, OutboxOperation.UPSERT, now)
            outboxDao.enqueue(SyncEntityType.EXERCISE, testUuid("2"), OutboxOperation.UPSERT, now)

            assertEquals(2, outboxDao.countPending())
        }

    @Test
    fun givenAPendingEntry_whenAttemptsAreIncremented_thenTheCounterGoesUpWithoutChangingTheRevision() =
        runTest {
            outboxDao.enqueue(SyncEntityType.WORKOUT, entityId, OutboxOperation.UPSERT, now)
            val before = outboxDao.findReadyToPush(now, 100).single()

            outboxDao.incrementAttempts(SyncEntityType.WORKOUT, entityId)
            outboxDao.incrementAttempts(SyncEntityType.WORKOUT, entityId)

            val after = outboxDao.findReadyToPush(now, 100).single()
            assertEquals(2, after.attempts)
            assertEquals(before.revision, after.revision)
        }

    @Test
    fun givenAFailingWriteTransaction_whenSavingANewWorkoutAndEnqueuing_thenNeitherThePersistedWorkoutNorTheOutboxRowSurvive() =
        runTest {
            val workoutId = testUuid("5")

            assertFailsWith<RollbackTestException> {
                database.withWriteTransaction {
                    database.workoutDao().saveLocalWorkout(workout(workoutId), emptyList(), emptyList())
                    outboxDao.enqueue(SyncEntityType.WORKOUT, workoutId, OutboxOperation.UPSERT, now)
                    throw RollbackTestException()
                }
            }

            assertNull(database.workoutDao().findWorkoutIncludingDeleted(workoutId))
            assertTrue(outboxDao.findPendingEntityIds(SyncEntityType.WORKOUT, listOf(workoutId)).isEmpty())
        }

    @Test
    fun givenAFailingWriteTransaction_whenUpdatingAnExistingWorkoutAndItsOutboxRow_thenBothAreLeftUnchanged() =
        runTest {
            val workoutId = testUuid("6")
            database.workoutDao().saveLocalWorkout(workout(workoutId, name = "Push day"), emptyList(), emptyList())
            outboxDao.enqueue(SyncEntityType.WORKOUT, workoutId, OutboxOperation.UPSERT, now)
            val revisionBeforeRollback = outboxDao.findReadyToPush(now, 100).single().revision

            assertFailsWith<RollbackTestException> {
                database.withWriteTransaction {
                    database.workoutDao().saveLocalWorkout(workout(workoutId, name = "Leg day"), emptyList(), emptyList())
                    outboxDao.enqueue(SyncEntityType.WORKOUT, workoutId, OutboxOperation.UPSERT, now + 60.seconds)
                    throw RollbackTestException()
                }
            }

            assertEquals(
                "Push day",
                database
                    .workoutDao()
                    .findWorkoutIncludingDeleted(workoutId)
                    ?.workout
                    ?.name,
            )
            assertEquals(revisionBeforeRollback, outboxDao.findReadyToPush(now, 100).single().revision)
        }

    private fun workout(
        id: Uuid,
        name: String = "Push day",
    ) = WorkoutEntity(
        id = id,
        templateId = null,
        name = name,
        notes = "",
        startedAt = now,
        finishedAt = now,
        updatedAt = now,
        serverVersion = null,
        isDeleted = false,
    )

    private class RollbackTestException : Exception()
}
