package com.repz.app.core.sync

import androidx.room3.Room
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.buildRepzDatabase
import com.repz.app.core.domain.session.SessionState
import com.repz.app.core.sync.engine.SyncEngine
import com.repz.app.core.sync.fakes.FakeSyncApi
import com.repz.app.core.sync.fakes.RecordingPersonalRecordsRefresher
import com.repz.app.core.sync.handler.SyncHandlerRegistry
import com.repz.app.core.sync.status.SyncStatusTracker
import com.repz.app.core.testing.session.FakeSessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.time.Instant

/** Room needs a real dispatcher for its query/invalidation work, not the runTest scheduler. */
internal fun inMemoryRepzDatabase(): RepzDatabase = Room.inMemoryDatabaseBuilder<RepzDatabase>().buildRepzDatabase(Dispatchers.Default)

/**
 * A fully-wired [SyncEngine] over an in-memory [RepzDatabase], for engine-level `given…_when…_then…`
 * tests. The database is deliberately never closed from within the test body: [statusTracker]'s
 * `status` keeps an eagerly-shared collector alive on the test's `backgroundScope` for the whole
 * `runTest` call, and closing the database while it is still running races a query against it.
 * `runTest` cancels `backgroundScope` as part of its own teardown, after the test body returns.
 */
internal class TestSyncEngineHarness(
    val database: RepzDatabase,
    val syncApi: FakeSyncApi,
    val sessionRepository: FakeSessionRepository,
    val recordsRefresher: RecordingPersonalRecordsRefresher,
    val statusTracker: SyncStatusTracker,
    val clock: MutableClock,
    val engine: SyncEngine,
)

internal fun testSyncEngine(
    applicationScope: CoroutineScope,
    database: RepzDatabase = inMemoryRepzDatabase(),
    syncApi: FakeSyncApi = FakeSyncApi(),
    sessionRepository: FakeSessionRepository = FakeSessionRepository(SessionState.SignedIn),
    recordsRefresher: RecordingPersonalRecordsRefresher = RecordingPersonalRecordsRefresher(),
    clock: MutableClock = MutableClock(Instant.fromEpochMilliseconds(0)),
): TestSyncEngineHarness {
    val statusTracker = SyncStatusTracker(database.outboxDao(), database.syncMetaDao(), applicationScope)
    val handlers = SyncHandlerRegistry(database.workoutDao(), database.templateDao(), database.exerciseDao())
    val engine =
        SyncEngine(
            database = database,
            outboxDao = database.outboxDao(),
            syncMetaDao = database.syncMetaDao(),
            syncRejectionDao = database.syncRejectionDao(),
            syncApi = syncApi,
            sessionRepository = sessionRepository,
            handlers = handlers,
            recordsRefresher = recordsRefresher,
            statusTracker = statusTracker,
            clock = clock,
        )
    return TestSyncEngineHarness(database, syncApi, sessionRepository, recordsRefresher, statusTracker, clock, engine)
}
