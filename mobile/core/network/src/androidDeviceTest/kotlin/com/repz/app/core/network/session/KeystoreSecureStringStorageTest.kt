package com.repz.app.core.network.session

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.repz.app.core.common.coroutines.AppDispatchers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

/**
 * Runs on a real device/emulator against the real Android Keystore (no fake, Robolectric can't
 * back a hardware-backed `AndroidKeyStore`). Each test starts and ends with a clean slate: the
 * stored entry removed and the Keystore key (alias [KEY_ALIAS]) deleted, so the suite doesn't leak
 * state across test methods or across runs on the same emulator.
 */
@RunWith(AndroidJUnit4::class)
class KeystoreSecureStringStorageTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dispatchers = AppDispatchers(default = Dispatchers.Default, io = Dispatchers.IO)
    private val storage = KeystoreSecureStringStorage(context, dispatchers)

    @Before
    @After
    fun clearState() {
        runBlocking { storage.delete() }
        deleteKeystoreKeyIfPresent()
    }

    @Test
    fun givenNothingStored_whenRead_thenReturnsNull() =
        runBlocking {
            assertNull(storage.read())
        }

    @Test
    fun givenAWrittenValue_whenRead_thenReturnsTheSameValue() =
        runBlocking {
            storage.write("first-session")

            assertEquals("first-session", storage.read())
        }

    @Test
    fun givenAnExistingValue_whenWrittenAgain_thenReadReturnsTheNewValue() =
        runBlocking {
            storage.write("first-session")
            storage.write("second-session")

            assertEquals("second-session", storage.read())
        }

    @Test
    fun givenAWrittenValue_whenDeleted_thenReadReturnsNull() =
        runBlocking {
            storage.write("first-session")

            storage.delete()

            assertNull(storage.read())
        }

    @Test
    fun givenAnUndecryptableEntry_whenRead_thenReturnsNullAndClearsTheEntry() =
        runBlocking {
            // Not a product of encrypt(): no IV/ciphertext delimiter, so decrypt() throws before it
            // ever touches the Keystore key.
            preferences().edit().putString(PREFERENCES_KEY, "not-a-valid-secure-entry").commit()

            assertNull(storage.read())
            assertNull(preferences().getString(PREFERENCES_KEY, null))
        }

    @Test
    fun givenAnInvalidatedKeystoreKey_whenRead_thenReturnsNullAndClearsTheEntry() =
        runBlocking {
            storage.write("first-session")

            // Simulates a key the OS invalidated (e.g. a restore to a new device): the
            // SharedPreferences entry survives, only the Keystore key is gone.
            deleteKeystoreKeyIfPresent()

            assertNull(storage.read())
            assertNull(preferences().getString(PREFERENCES_KEY, null))
        }

    private fun preferences() = context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)

    private fun deleteKeystoreKeyIfPresent() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
    }
}
