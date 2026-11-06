package com.repz.app.core.sync.status

import androidx.room3.Room
import app.cash.turbine.test
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.buildRepzDatabase
import com.repz.app.core.database.sync.OutboxOperation
import com.repz.app.core.database.sync.SyncEntityType
import com.repz.app.core.database.sync.SyncMetaEntity
import com.repz.app.core.domain.sync.RestoreProgress
import com.repz.app.core.domain.sync.RestoreState
import com.repz.app.core.domain.sync.SyncFailureReason
import com.repz.app.core.domain.sync.SyncStatus
import com.repz.app.core.sync.engine.SyncOutcome
import com.repz.app.core.sync.testUuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SyncStatusTrackerTest {
    // Room needs a real dispatcher for its query/invalidation work, not the runTest scheduler.
    private val database = Room.inMemoryDatabaseBuilder<RepzDatabase>().buildRepzDatabase(Dispatchers.Default)

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun givenARunStarts_whenObservingStatus_thenEmitsSyncing() =
        runTest {
            val tracker = SyncStatusTracker(database.outboxDao(), database.syncMetaDao(), backgroundScope)

            tracker.status.test {
                assertEquals(SyncStatus.Idle(null), awaitItem())
                tracker.onRunStarted()
                assertEquals(SyncStatus.Syncing, awaitItem())
            }
        }

    @Test
    fun givenARunFinishesOffline_whenObservingStatus_thenEmitsOfflineWithThePendingCount() =
        runTest {
            val tracker = SyncStatusTracker(database.outboxDao(), database.syncMetaDao(), backgroundScope)

            tracker.status.test {
                assertEquals(SyncStatus.Idle(null), awaitItem())
                database.outboxDao().enqueue(
                    SyncEntityType.WORKOUT,
                    testUuid("1"),
                    OutboxOperation.UPSERT,
                    Instant.fromEpochMilliseconds(0),
                )
                tracker.onRunStarted()
                assertEquals(SyncStatus.Syncing, awaitItem())
                tracker.onRunFinished(SyncOutcome.RetryLater(null))
                assertEquals(SyncStatus.Offline(1), awaitItem())
            }
        }

    @Test
    fun givenARunFailsWithoutARetryReason_whenObservingStatus_thenEmitsError() =
        runTest {
            val tracker = SyncStatusTracker(database.outboxDao(), database.syncMetaDao(), backgroundScope)

            tracker.status.test {
                assertEquals(SyncStatus.Idle(null), awaitItem())
                tracker.onRunStarted()
                assertEquals(SyncStatus.Syncing, awaitItem())
                tracker.onRunFinished(SyncOutcome.Failed(SyncFailureReason.Unexpected))
                assertEquals(SyncStatus.Error(SyncFailureReason.Unexpected), awaitItem())
            }
        }

    @Test
    fun givenARunFailsWithARetryReason_whenObservingStatus_thenEmitsError() =
        runTest {
            val tracker = SyncStatusTracker(database.outboxDao(), database.syncMetaDao(), backgroundScope)

            tracker.status.test {
                assertEquals(SyncStatus.Idle(null), awaitItem())
                tracker.onRunStarted()
                assertEquals(SyncStatus.Syncing, awaitItem())
                tracker.onRunFinished(SyncOutcome.RetryLater(SyncFailureReason.ServerUnavailable))
                assertEquals(SyncStatus.Error(SyncFailureReason.ServerUnavailable), awaitItem())
            }
        }

    @Test
    fun givenARunCompletes_whenObservingStatus_thenEmitsIdleWithTheLastSuccessfulSyncTime() =
        runTest {
            val tracker = SyncStatusTracker(database.outboxDao(), database.syncMetaDao(), backgroundScope)
            val lastSync = Instant.fromEpochMilliseconds(1_000)

            tracker.status.test {
                assertEquals(SyncStatus.Idle(null), awaitItem())
                tracker.onRunStarted()
                assertEquals(SyncStatus.Syncing, awaitItem())
                // The meta write and the run-finished signal are two independent causes: triggering
                // them one at a time (instead of racing both before a single awaitItem) keeps each
                // resulting emission unambiguous.
                tracker.onRunFinished(SyncOutcome.Completed)
                assertEquals(SyncStatus.Idle(null), awaitItem())
                database.syncMetaDao().save(SyncMetaEntity.INITIAL.copy(lastSuccessfulSyncAt = lastSync))
                assertEquals(SyncStatus.Idle(lastSync), awaitItem())
            }
        }

    @Test
    fun givenNoRestoreCompletedYet_whenObservingRestoreState_thenInProgressWithTheLatestProgress() =
        runTest {
            val tracker = SyncStatusTracker(database.outboxDao(), database.syncMetaDao(), backgroundScope)

            tracker.restoreState.test {
                assertEquals(RestoreState.InProgress(null), awaitItem())
                val progress = RestoreProgress(restoredWorkouts = 1, totalWorkouts = 10, restoredChanges = 5, totalChanges = 50)
                tracker.onRestoreProgress(progress)
                assertEquals(RestoreState.InProgress(progress), awaitItem())
            }
        }

    @Test
    fun givenInitialPullCompletedAtIsSet_whenObservingRestoreState_thenCompleted() =
        runTest {
            val tracker = SyncStatusTracker(database.outboxDao(), database.syncMetaDao(), backgroundScope)

            tracker.restoreState.test {
                assertEquals(RestoreState.InProgress(null), awaitItem())
                database.syncMetaDao().save(SyncMetaEntity.INITIAL.copy(initialPullCompletedAt = Instant.fromEpochMilliseconds(500)))
                assertEquals(RestoreState.Completed, awaitItem())
            }
        }
}
