package com.repz.app.core.network.session

import com.repz.app.core.network.testing.InMemorySecureStringStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PersistentTokenStoreTest {
    @Test
    fun givenNothingStored_whenLoadIsCalled_thenItReturnsNull() =
        runTest {
            val store = PersistentTokenStore(InMemorySecureStringStorage())

            assertNull(store.load())
        }

    @Test
    fun givenTokensWereSaved_whenLoaded_thenTheyRoundTrip() =
        runTest {
            val storage = InMemorySecureStringStorage()
            val store = PersistentTokenStore(storage)
            val tokens = AuthTokens(AccessToken("access-1"), RefreshToken("refresh-1"))

            val saveResult = store.save(tokens)
            val loaded = store.load()

            assertEquals(TokenSaveResult.Saved, saveResult)
            assertEquals(tokens, loaded)
        }

    @Test
    fun givenTokensAreSaved_whenInspectingTheUnderlyingStorage_thenThereIsExactlyOneEntry() =
        runTest {
            val storage = InMemorySecureStringStorage()
            val store = PersistentTokenStore(storage)

            store.save(AuthTokens(AccessToken("access-1"), RefreshToken("refresh-1")))

            // One atomic JSON entry holds both tokens; not two separate keys.
            assertEquals(1, listOfNotNull(storage.read()).size)
        }

    @Test
    fun givenACorruptStoredValue_whenLoaded_thenItIsDeletedAndNullIsReturned() =
        runTest {
            val storage = InMemorySecureStringStorage()
            storage.write("not valid json at all")
            val store = PersistentTokenStore(storage)

            val loaded = store.load()

            assertNull(loaded)
            assertNull(storage.read())
        }

    @Test
    fun givenAnOldFormatStoredValue_whenLoaded_thenItIsDeletedAndNullIsReturned() =
        runTest {
            val storage = InMemorySecureStringStorage()
            storage.write("""{"version":0,"accessToken":"a","refreshToken":"r"}""")
            val store = PersistentTokenStore(storage)

            val loaded = store.load()

            assertNull(loaded)
            assertNull(storage.read())
        }

    @Test
    fun givenTokensWereSavedThenCleared_whenLoaded_thenItReturnsNull() =
        runTest {
            val storage = InMemorySecureStringStorage()
            val store = PersistentTokenStore(storage)
            store.save(AuthTokens(AccessToken("access-1"), RefreshToken("refresh-1")))

            store.clear()

            assertNull(store.load())
        }
}
