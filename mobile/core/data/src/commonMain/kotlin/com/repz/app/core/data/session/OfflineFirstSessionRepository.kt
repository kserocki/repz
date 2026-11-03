package com.repz.app.core.data.session

import com.repz.app.core.domain.session.SessionRepository
import com.repz.app.core.domain.session.SessionState
import com.repz.app.core.network.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Over `core:network`'s [SessionManager]; [scope] is the app-lifetime scope so [sessionState] keeps observing for the process's life. */
class OfflineFirstSessionRepository(
    private val sessionManager: SessionManager,
    scope: CoroutineScope,
) : SessionRepository {
    override val sessionState: StateFlow<SessionState> =
        sessionManager.status
            .map { it.toSessionState() }
            .stateIn(scope, SharingStarted.Eagerly, sessionManager.status.value.toSessionState())

    override suspend fun restore(): SessionState = sessionManager.restore().toSessionState()
}
