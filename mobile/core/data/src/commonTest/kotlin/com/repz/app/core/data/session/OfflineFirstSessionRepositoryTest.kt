package com.repz.app.core.data.session

import app.cash.turbine.test
import co.touchlab.kermit.Logger
import com.repz.app.core.domain.session.SessionState
import com.repz.app.core.domain.session.SignOutReason
import com.repz.app.core.network.session.AccessToken
import com.repz.app.core.network.session.AuthTokens
import com.repz.app.core.network.session.RefreshToken
import com.repz.app.core.network.session.SessionManager
import com.repz.app.core.network.session.TokenSaveResult
import com.repz.app.core.network.session.TokenStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * An API fake, not a domain repository fake (konsist A5): `TokenStore` belongs to `core:network`,
 * so its test double lives in `core:data`'s own tests, not in `core:testing`.
 */
private class FakeTokenStore(
    private var stored: AuthTokens? = null,
) : TokenStore {
    override suspend fun load(): AuthTokens? = stored

    override suspend fun save(tokens: AuthTokens): TokenSaveResult {
        stored = tokens
        return TokenSaveResult.Saved
    }

    override suspend fun clear() {
        stored = null
    }
}

private fun sampleTokens() = AuthTokens(AccessToken("access"), RefreshToken("refresh"))

private fun testLogger(): Logger = Logger.withTag("OfflineFirstSessionRepositoryTest")

class OfflineFirstSessionRepositoryTest {
    @Test
    fun givenAFreshSessionManager_whenRepositoryIsCreated_thenSessionStateStartsUnknown() =
        runTest {
            val manager = SessionManager(FakeTokenStore(), testLogger())
            val repository = OfflineFirstSessionRepository(manager, backgroundScope)

            assertEquals(SessionState.Unknown, repository.sessionState.value)
        }

    @Test
    fun givenNoStoredSession_whenRestoreIsCalled_thenItReturnsAndEmitsSignedOutNoSession() =
        runTest {
            val manager = SessionManager(FakeTokenStore(), testLogger())
            val repository = OfflineFirstSessionRepository(manager, backgroundScope)

            repository.sessionState.test {
                assertEquals(SessionState.Unknown, awaitItem())

                val result = repository.restore()

                assertEquals(SessionState.SignedOut(SignOutReason.NoSession), result)
                assertEquals(SessionState.SignedOut(SignOutReason.NoSession), awaitItem())
            }
        }

    @Test
    fun givenAStoredSession_whenRestoreIsCalled_thenItReturnsAndEmitsSignedIn() =
        runTest {
            val manager = SessionManager(FakeTokenStore(sampleTokens()), testLogger())
            val repository = OfflineFirstSessionRepository(manager, backgroundScope)

            repository.sessionState.test {
                assertEquals(SessionState.Unknown, awaitItem())

                val result = repository.restore()

                assertEquals(SessionState.SignedIn, result)
                assertEquals(SessionState.SignedIn, awaitItem())
            }
        }

    @Test
    fun givenRestoreAlreadyRan_whenRestoreIsCalledAgain_thenItDelegatesToTheSessionManagerWithoutReloading() =
        runTest {
            val store = FakeTokenStore()
            val manager = SessionManager(store, testLogger())
            val repository = OfflineFirstSessionRepository(manager, backgroundScope)
            repository.restore()
            store.save(sampleTokens())

            val secondResult = repository.restore()

            assertEquals(SessionState.SignedOut(SignOutReason.NoSession), secondResult)
        }

    @Test
    fun givenTheSessionManagerSignsInAndLogsOut_whenNoRestoreWasCalled_thenSessionStateFollowsBothTransitions() =
        runTest {
            val manager = SessionManager(FakeTokenStore(), testLogger())
            val repository = OfflineFirstSessionRepository(manager, backgroundScope)

            repository.sessionState.test {
                assertEquals(SessionState.Unknown, awaitItem())

                manager.start(sampleTokens())
                assertEquals(SessionState.SignedIn, awaitItem())

                manager.end()
                assertEquals(SessionState.SignedOut(SignOutReason.LoggedOut), awaitItem())
            }
        }
}
