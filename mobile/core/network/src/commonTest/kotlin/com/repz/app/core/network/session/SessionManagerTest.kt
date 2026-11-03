package com.repz.app.core.network.session

import com.repz.app.core.network.testing.InMemorySecureStringStorage
import com.repz.app.core.network.testing.testLogger
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private class FailingTokenStore : TokenStore {
    var lastSaved: AuthTokens? = null

    override suspend fun load(): AuthTokens? = null

    override suspend fun save(tokens: AuthTokens): TokenSaveResult {
        lastSaved = tokens
        return TokenSaveResult.Failed
    }

    override suspend fun clear() = Unit
}

private fun sampleTokens(suffix: String = "1") = AuthTokens(AccessToken("access-$suffix"), RefreshToken("refresh-$suffix"))

class SessionManagerTest {
    @Test
    fun givenAFreshManager_whenRestoreIsCalledWithNoStoredSession_thenStatusBecomesSignedOutNoSession() =
        runTest {
            val manager = SessionManager(PersistentTokenStore(InMemorySecureStringStorage()), testLogger())

            val status = manager.restore()

            assertEquals(SessionStatus.SignedOut(SignOutReason.NoSession), status)
            assertEquals(SessionStatus.SignedOut(SignOutReason.NoSession), manager.status.value)
        }

    @Test
    fun givenAStoredSession_whenRestoreIsCalled_thenStatusBecomesSignedIn() =
        runTest {
            val storage = InMemorySecureStringStorage()
            val store = PersistentTokenStore(storage)
            store.save(sampleTokens())
            val manager = SessionManager(store, testLogger())

            val status = manager.restore()

            assertEquals(SessionStatus.SignedIn, status)
        }

    @Test
    fun givenRestoreAlreadyRan_whenRestoreIsCalledAgain_thenItReturnsTheSameStatusWithoutReloading() =
        runTest {
            val storage = InMemorySecureStringStorage()
            val store = PersistentTokenStore(storage)
            val manager = SessionManager(store, testLogger())
            manager.restore()
            store.save(sampleTokens())

            val secondStatus = manager.restore()

            assertEquals(SessionStatus.SignedOut(SignOutReason.NoSession), secondStatus)
        }

    @Test
    fun givenASuccessfulStart_whenStartIsCalled_thenStatusBecomesSignedInAndTokensArePersisted() =
        runTest {
            val storage = InMemorySecureStringStorage()
            val store = PersistentTokenStore(storage)
            val manager = SessionManager(store, testLogger())

            val result = manager.start(sampleTokens())

            assertEquals(TokenSaveResult.Saved, result)
            assertEquals(SessionStatus.SignedIn, manager.status.value)
            assertEquals(sampleTokens(), store.load())
        }

    @Test
    fun givenEnd_whenCalledAfterStart_thenStatusBecomesSignedOutLoggedOutAndStoreIsCleared() =
        runTest {
            val storage = InMemorySecureStringStorage()
            val store = PersistentTokenStore(storage)
            val manager = SessionManager(store, testLogger())
            manager.start(sampleTokens())

            manager.end()

            assertEquals(SessionStatus.SignedOut(SignOutReason.LoggedOut), manager.status.value)
            assertEquals(null, store.load())
            assertEquals(null, manager.currentRefreshToken())
        }

    @Test
    fun givenTheStoreFailsToSave_whenStartIsCalled_thenTheSessionStaysUsableInMemoryAndReturnsFailed() =
        runTest {
            val failingStore = FailingTokenStore()
            val manager = SessionManager(failingStore, testLogger())

            val result = manager.start(sampleTokens())

            assertEquals(TokenSaveResult.Failed, result)
            assertEquals(SessionStatus.SignedIn, manager.status.value)
            assertEquals(RefreshToken("refresh-1"), manager.currentRefreshToken())
        }

    @Test
    fun givenASignedInSession_whenExpireIsCalled_thenStatusBecomesSignedOutExpiredAndStoreIsCleared() =
        runTest {
            val storage = InMemorySecureStringStorage()
            val store = PersistentTokenStore(storage)
            val manager = SessionManager(store, testLogger())
            manager.start(sampleTokens())

            manager.expire()

            val status = manager.status.value
            assertIs<SessionStatus.SignedOut>(status)
            assertEquals(SignOutReason.Expired, status.reason)
            assertEquals(null, store.load())
        }

    @Test
    fun givenMatchingExpectedTokens_whenReplaceIsCalled_thenTokensAreReplacedAndTrueIsReturned() =
        runTest {
            val storage = InMemorySecureStringStorage()
            val store = PersistentTokenStore(storage)
            val manager = SessionManager(store, testLogger())
            val original = sampleTokens("1")
            manager.start(original)

            val replaced = manager.replace(original, sampleTokens("2"))

            assertEquals(true, replaced)
            assertEquals(sampleTokens("2"), manager.currentTokens())
        }

    @Test
    fun givenStaleExpectedTokens_whenReplaceIsCalled_thenNothingChangesAndFalseIsReturned() =
        runTest {
            val storage = InMemorySecureStringStorage()
            val store = PersistentTokenStore(storage)
            val manager = SessionManager(store, testLogger())
            manager.start(sampleTokens("1"))
            manager.end()

            val replaced = manager.replace(sampleTokens("1"), sampleTokens("2"))

            assertEquals(false, replaced)
            assertEquals(null, manager.currentTokens())
        }

    @Test
    fun givenTheStoreFailsToSave_whenReplaceIsCalled_thenTheRotatedTokensAreStillKeptInMemory() =
        runTest {
            val failingStore = FailingTokenStore()
            val manager = SessionManager(failingStore, testLogger())
            val original = sampleTokens("1")
            manager.start(original)

            val replaced = manager.replace(original, sampleTokens("2"))

            assertEquals(true, replaced)
            assertEquals(sampleTokens("2"), manager.currentTokens())
            assertEquals(sampleTokens("2"), failingStore.lastSaved)
        }
}
