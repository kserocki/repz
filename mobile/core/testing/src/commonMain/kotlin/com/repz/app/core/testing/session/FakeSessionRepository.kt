package com.repz.app.core.testing.session

import com.repz.app.core.domain.session.SessionRepository
import com.repz.app.core.domain.session.SessionState
import com.repz.app.core.domain.session.SignOutReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A hand-written fake (N7): [restore] moves [Unknown][SessionState.Unknown] to [restoredState]; [emit] drives later transitions. */
class FakeSessionRepository(
    private val restoredState: SessionState = SessionState.SignedOut(SignOutReason.NoSession),
) : SessionRepository {
    private val mutableSessionState = MutableStateFlow<SessionState>(SessionState.Unknown)
    override val sessionState: StateFlow<SessionState> = mutableSessionState.asStateFlow()

    var restoreCallCount: Int = 0
        private set

    override suspend fun restore(): SessionState {
        restoreCallCount++
        if (mutableSessionState.value == SessionState.Unknown) {
            mutableSessionState.value = restoredState
        }
        return mutableSessionState.value
    }

    /** Emits a new state directly, e.g. to simulate a login, logout or an expired refresh. */
    fun emit(state: SessionState) {
        mutableSessionState.value = state
    }
}
