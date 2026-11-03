package com.repz.app.shared.shell

import app.cash.turbine.test
import com.repz.app.core.domain.session.SessionState
import com.repz.app.core.domain.session.SignOutReason
import com.repz.app.core.testing.session.FakeSessionRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [AuthGate]'s eager `stateIn` never completes on its own (it keeps collecting for the scope's
 * whole lifetime, by design): passed `runTest`'s own test-body scope, that leftover job would
 * fail the test with `UncompletedCoroutinesError`. `TestScope.backgroundScope` is built for
 * exactly this — a scope sharing the same scheduler whose coroutines are cancelled when the test
 * ends instead of being awaited to completion.
 */
class AuthGateTest {
    @Test
    fun givenUnknownSession_whenCreated_thenStateStartsRestoringThenResolves() =
        runTest {
            val sessionRepository = FakeSessionRepository(restoredState = SessionState.SignedOut(SignOutReason.NoSession))
            val authGate = AuthGate(sessionRepository, applicationScope = backgroundScope)

            authGate.state.test {
                assertEquals(AuthGateUiState.Restoring, awaitItem())
                assertEquals(AuthGateUiState.Ready(isSignedIn = false), awaitItem())
            }
        }

    @Test
    fun givenAnySession_whenCreated_thenRestoreIsCalledExactlyOnce() =
        runTest {
            val sessionRepository = FakeSessionRepository()
            val authGate = AuthGate(sessionRepository, applicationScope = backgroundScope)

            authGate.state.test {
                awaitItem()
                awaitItem()
            }

            assertEquals(1, sessionRepository.restoreCallCount)
        }

    @Test
    fun givenStoredSignedInSession_whenCreated_thenStateResolvesToReadySignedIn() =
        runTest {
            val sessionRepository = FakeSessionRepository(restoredState = SessionState.SignedIn)
            val authGate = AuthGate(sessionRepository, applicationScope = backgroundScope)

            authGate.state.test {
                assertEquals(AuthGateUiState.Restoring, awaitItem())
                assertEquals(AuthGateUiState.Ready(isSignedIn = true), awaitItem())
            }
        }

    @Test
    fun givenSignedIn_whenSessionExpires_thenStateFollowsToReadySignedOut() =
        runTest {
            val sessionRepository = FakeSessionRepository(restoredState = SessionState.SignedIn)
            val authGate = AuthGate(sessionRepository, applicationScope = backgroundScope)

            authGate.state.test {
                assertEquals(AuthGateUiState.Restoring, awaitItem())
                assertEquals(AuthGateUiState.Ready(isSignedIn = true), awaitItem())

                sessionRepository.emit(SessionState.SignedOut(SignOutReason.Expired))

                assertEquals(AuthGateUiState.Ready(isSignedIn = false), awaitItem())
            }
        }
}
