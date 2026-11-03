package com.repz.app.core.data.session

import com.repz.app.core.domain.session.SessionState
import com.repz.app.core.domain.session.SignOutReason
import com.repz.app.core.network.session.SessionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import com.repz.app.core.network.session.SignOutReason as NetworkSignOutReason

/**
 * The pure mapper covers all 5 [SessionState] values, including [SignOutReason.Expired]: that
 * status is only ever reached inside `core:network` itself (`SessionManager.expire` is internal,
 * reachable only from a failed token refresh), so it can't be driven through a live session
 * manager from this module.
 */
class SessionStateMappingTest {
    @Test
    fun givenSessionStatusUnknown_whenMapped_thenSessionStateIsUnknown() {
        assertEquals(SessionState.Unknown, SessionStatus.Unknown.toSessionState())
    }

    @Test
    fun givenSessionStatusSignedIn_whenMapped_thenSessionStateIsSignedIn() {
        assertEquals(SessionState.SignedIn, SessionStatus.SignedIn.toSessionState())
    }

    @Test
    fun givenSessionStatusSignedOutNoSession_whenMapped_thenSessionStateIsSignedOutNoSession() {
        assertEquals(
            SessionState.SignedOut(SignOutReason.NoSession),
            SessionStatus.SignedOut(NetworkSignOutReason.NoSession).toSessionState(),
        )
    }

    @Test
    fun givenSessionStatusSignedOutLoggedOut_whenMapped_thenSessionStateIsSignedOutLoggedOut() {
        assertEquals(
            SessionState.SignedOut(SignOutReason.LoggedOut),
            SessionStatus.SignedOut(NetworkSignOutReason.LoggedOut).toSessionState(),
        )
    }

    @Test
    fun givenSessionStatusSignedOutExpired_whenMapped_thenSessionStateIsSignedOutExpired() {
        assertEquals(
            SessionState.SignedOut(SignOutReason.Expired),
            SessionStatus.SignedOut(NetworkSignOutReason.Expired).toSessionState(),
        )
    }
}
