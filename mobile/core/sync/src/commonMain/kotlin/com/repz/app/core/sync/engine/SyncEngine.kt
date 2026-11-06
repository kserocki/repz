package com.repz.app.core.sync.engine

import co.touchlab.kermit.Logger
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.sync.OutboxDao
import com.repz.app.core.database.sync.SyncMetaDao
import com.repz.app.core.database.sync.SyncRejectionDao
import com.repz.app.core.domain.session.SessionRepository
import com.repz.app.core.domain.session.SessionState
import com.repz.app.core.network.api.SyncApi
import com.repz.app.core.sync.handler.SyncHandlerRegistry
import com.repz.app.core.sync.logging.SYNC_LOG_TAG
import com.repz.app.core.sync.record.PersonalRecordsRefresher
import com.repz.app.core.sync.status.SyncStatusTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Pushes the outbox, then pulls pages until caught up, then pushes once more in case a row became
 * ready during the pull (ARCHITECTURE §5.3). [OutboxPusher] and [ChangePuller] wrap each push
 * response and each pull page in its own [RepzDatabase.withWriteTransaction].
 *
 * [stop] is cooperative, not a coroutine cancellation: a flag checked between transactional units
 * of work (one push batch, one pull page), so a stopped run always leaves the database at a
 * transaction boundary and `sync()` itself returns a normal [SyncOutcome.Stopped] value rather than
 * throwing - Konsist E1 bans a hand-written catch of `CancellationException` that doesn't rethrow it.
 */
@Single
class SyncEngine(
    database: RepzDatabase,
    outboxDao: OutboxDao,
    private val syncMetaDao: SyncMetaDao,
    syncRejectionDao: SyncRejectionDao,
    syncApi: SyncApi,
    private val sessionRepository: SessionRepository,
    handlers: SyncHandlerRegistry,
    recordsRefresher: PersonalRecordsRefresher,
    private val statusTracker: SyncStatusTracker,
    private val clock: Clock,
    private val logger: Logger = Logger.withTag(SYNC_LOG_TAG),
) {
    private val mutex = Mutex()
    private val stopRequested = MutableStateFlow(false)

    private val outboxPusher =
        OutboxPusher(database, outboxDao, syncRejectionDao, syncApi, handlers, recordsRefresher, clock, logger)
    private val changePuller =
        ChangePuller(database, syncMetaDao, outboxDao, syncApi, handlers, recordsRefresher, statusTracker, clock, logger)

    /** Serialised by a Mutex: concurrent callers run one after another; idempotent. */
    suspend fun sync(): SyncOutcome =
        mutex.withLock {
            stopRequested.value = false
            if (sessionRepository.restore() !is SessionState.SignedIn) {
                return@withLock SyncOutcome.SkippedSignedOut
            }

            statusTracker.onRunStarted()
            logger.i { "sync: run started" }
            val outcome = runCycle()
            logger.i { "sync: run finished with $outcome" }
            statusTracker.onRunFinished(outcome)
            outcome
        }

    /** Cancels the in-flight run (if any) and waits for it; that run then returns [SyncOutcome.Stopped]. */
    suspend fun stop() {
        stopRequested.value = true
        mutex.withLock { }
    }

    /** Push, pull, push again: three early-return stages, the clearest shape for "stop here, or keep going". */
    @Suppress("ReturnCount")
    private suspend fun runCycle(): SyncOutcome {
        outboxPusher.pushReadyChanges(::isStopRequested)?.let { return it }
        changePuller.pullUntilDone(::isStopRequested)?.let { return it }
        // Final push pass: a row may have become ready while the pull was running.
        outboxPusher.pushReadyChanges(::isStopRequested)?.let { return it }

        syncMetaDao.save(syncMetaDao.get().copy(lastSuccessfulSyncAt = clock.now()))
        return SyncOutcome.Completed
    }

    private fun isStopRequested(): Boolean = stopRequested.value
}
