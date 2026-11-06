package com.repz.app.core.datastore

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSFileManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Exercises the real platform path: a file-backed preferences DataStore under Application Support. */
class RepzPreferencesDataStoreIosTest {
    @OptIn(ExperimentalForeignApi::class)
    private val preferencesPath = "${applicationSupportDirectoryPath()}/$REPZ_PREFERENCES_FILE_NAME"

    @BeforeTest
    fun deleteStalePreferencesFile() = deletePreferencesFile()

    @AfterTest
    fun deletePreferencesFileAfterTest() = deletePreferencesFile()

    @Test
    fun givenApplicationSupportDirectory_whenPreferencesWritten_thenFileIsCreatedAndReadable() =
        runTest {
            val dataSource = UserPreferencesDataSource(repzPreferencesDataStore(Dispatchers.IO))

            dataSource.setHapticsEnabled(true)
            dataSource.setWeeklyWorkoutGoal(5)
            val stored = dataSource.preferences.first()

            assertTrue(NSFileManager.defaultManager.fileExistsAtPath(preferencesPath))
            assertEquals(true, stored.hapticsEnabled)
            assertEquals(5, stored.weeklyWorkoutGoal)
        }

    @OptIn(ExperimentalForeignApi::class)
    private fun deletePreferencesFile() {
        NSFileManager.defaultManager.removeItemAtPath(preferencesPath, error = null)
    }
}
