package com.repz.app.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Runs on a real device/emulator against the app's real private files directory: the only way to
 * prove [repzPreferencesDataStore] resolves to a real, writable file there (a host JVM test has no
 * `Context`). DataStore allows only one active instance per file per process, so this stays a
 * single test built around one instance of the real builder instead of simulating a relaunch.
 */
@RunWith(AndroidJUnit4::class)
class RepzPreferencesDataStoreDeviceTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val testKey = stringPreferencesKey("repz_preferences_data_store_device_test_key")
    private val preferencesFile = File(context.filesDir, "datastore/$REPZ_PREFERENCES_FILE_NAME")

    @Before
    @After
    fun clearState() {
        preferencesFile.delete()
    }

    @Test
    fun givenTheRealFileBackedDataStore_whenWritingThenReading_thenTheValueRoundTripsThroughTheRealFile() =
        runBlocking {
            val dataStore = repzPreferencesDataStore(context, Dispatchers.IO)

            assertNull(dataStore.data.first()[testKey])

            dataStore.edit { it[testKey] = "on-disk-value" }

            assertEquals("on-disk-value", dataStore.data.first()[testKey])
            assertTrue(preferencesFile.exists())
        }
}
