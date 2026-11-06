package com.repz.app.core.sync.engine

import co.touchlab.kermit.Logger
import com.repz.app.core.domain.session.SessionRepository
import com.repz.app.core.sync.logging.SYNC_LOG_TAG
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single

/**
 * Pushes the outbox, then pulls pages until caught up (ARCH §5.3). This first version only
 * performs the session check so `core:sync`'s public shape (the scheduling types and this class)
 * is ready to build against; a following change adds the handler registry, status tracking and
 * the full push/pull algorithm this will run.
 */
@Single
class SyncEngine(
    private val sessionRepository: SessionRepository,
    private val logger: Logger = Logger.withTag(SYNC_LOG_TAG),
) {
    private val mutex = Mutex()

    /** Serialised by [mutex]: concurrent callers run one after another; idempotent. */
    suspend fun sync(): SyncOutcome =
        mutex.withLock {
            sessionRepository.restore()
            logger.i { "sync: session checked, the push/pull algorithm isn't wired up yet" }
            SyncOutcome.SkippedSignedOut
        }

    /** Cancels the in-flight run (if any) and waits for it; that run then returns [SyncOutcome.Stopped]. */
    suspend fun stop() {
        // No run outlives sync() yet; a following change adds the cancellable job this stops.
    }
}
