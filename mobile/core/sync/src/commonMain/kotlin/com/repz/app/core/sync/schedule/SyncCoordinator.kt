package com.repz.app.core.sync.schedule

import com.repz.app.core.domain.session.SessionRepository
import com.repz.app.core.domain.session.SessionState
import com.repz.app.core.sync.engine.SyncEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

/**
 * Starts the engine's background triggers for the app's lifetime (ARCHITECTURE §5.6/§5.7): the
 * platform scheduler is started once, then the session state drives the periodic schedule and,
 * on logout, cancels every scheduled run and stops any run in flight.
 */
@Single
class SyncCoordinator(
    private val sessionRepository: SessionRepository,
    private val scheduler: BackgroundSyncScheduler,
    private val engine: SyncEngine,
    private val applicationScope: CoroutineScope,
) {
    /** Synchronous; called once per process by `startRepzSync()`. */
    fun start() {
        scheduler.start()
        applicationScope.launch {
            sessionRepository.sessionState.collect { state ->
                when (state) {
                    SessionState.SignedIn -> {
                        scheduler.schedulePeriodic()
                    }

                    is SessionState.SignedOut -> {
                        scheduler.cancelAll()
                        engine.stop()
                    }

                    SessionState.Unknown -> {}
                }
            }
        }
    }
}
