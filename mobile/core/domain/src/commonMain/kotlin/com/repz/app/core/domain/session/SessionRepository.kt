package com.repz.app.core.domain.session

import kotlinx.coroutines.flow.StateFlow

/**
 * The auth gate's one source of truth (ADR-0025). Implemented in `core:data` over
 * `core:network`'s `SessionManager`; login, register and logout live elsewhere and end in
 * `SessionManager.start()`/`end()`, which this repository only observes.
 */
interface SessionRepository {
    val sessionState: StateFlow<SessionState>

    /** Reads the stored session once; later calls return the current state. */
    suspend fun restore(): SessionState
}
