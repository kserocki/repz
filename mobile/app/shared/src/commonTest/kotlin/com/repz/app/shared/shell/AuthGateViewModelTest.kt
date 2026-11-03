package com.repz.app.shared.shell

import app.cash.turbine.test
import com.repz.app.core.domain.session.SessionState
import com.repz.app.core.domain.session.SignOutReason
import com.repz.app.core.testing.session.FakeSessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `viewModelScope` runs on `Dispatchers.Main`, so every test shares [testDispatcher]'s scheduler
 * with `runTest`: a `StateFlow`'s current value emits synchronously when collection starts
 * (Turbine's `UNDISPATCHED` launch), and `runTest` auto-advances the shared scheduler whenever the
 * test body suspends waiting for the next one.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthGateViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun givenUnknownSession_whenCreated_thenStateStartsRestoringThenResolves() =
        runTest(testDispatcher) {
            val sessionRepository = FakeSessionRepository(restoredState = SessionState.SignedOut(SignOutReason.NoSession))
            val viewModel = AuthGateViewModel(sessionRepository)

            viewModel.state.test {
                assertEquals(AuthGateUiState.Restoring, awaitItem())
                assertEquals(AuthGateUiState.Ready(isSignedIn = false), awaitItem())
            }
        }

    @Test
    fun givenAnySession_whenCreated_thenRestoreIsCalledExactlyOnce() =
        runTest(testDispatcher) {
            val sessionRepository = FakeSessionRepository()
            val viewModel = AuthGateViewModel(sessionRepository)

            viewModel.state.test {
                awaitItem()
                awaitItem()
            }

            assertEquals(1, sessionRepository.restoreCallCount)
        }

    @Test
    fun givenStoredSignedInSession_whenCreated_thenStateResolvesToReadySignedIn() =
        runTest(testDispatcher) {
            val sessionRepository = FakeSessionRepository(restoredState = SessionState.SignedIn)
            val viewModel = AuthGateViewModel(sessionRepository)

            viewModel.state.test {
                assertEquals(AuthGateUiState.Restoring, awaitItem())
                assertEquals(AuthGateUiState.Ready(isSignedIn = true), awaitItem())
            }
        }

    @Test
    fun givenSignedIn_whenSessionExpires_thenStateFollowsToReadySignedOut() =
        runTest(testDispatcher) {
            val sessionRepository = FakeSessionRepository(restoredState = SessionState.SignedIn)
            val viewModel = AuthGateViewModel(sessionRepository)

            viewModel.state.test {
                assertEquals(AuthGateUiState.Restoring, awaitItem())
                assertEquals(AuthGateUiState.Ready(isSignedIn = true), awaitItem())

                sessionRepository.emit(SessionState.SignedOut(SignOutReason.Expired))

                assertEquals(AuthGateUiState.Ready(isSignedIn = false), awaitItem())
            }
        }
}
