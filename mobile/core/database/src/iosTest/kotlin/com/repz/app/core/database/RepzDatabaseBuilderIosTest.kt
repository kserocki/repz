package com.repz.app.core.database

import com.repz.app.core.database.sync.SyncMetaEntity
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSFileManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Exercises the real platform path: a file-backed [RepzDatabase] under Application Support. */
class RepzDatabaseBuilderIosTest {
    @OptIn(ExperimentalForeignApi::class)
    private val databasePath = "${applicationSupportDirectoryPath()}/$REPZ_DATABASE_FILE_NAME"

    @BeforeTest
    fun deleteStaleDatabaseFile() = deleteDatabaseFile()

    @AfterTest
    fun deleteDatabaseFileAfterTest() = deleteDatabaseFile()

    @Test
    fun givenApplicationSupportDirectory_whenRepzDatabaseBuilt_thenFileIsCreatedAndQueryable() =
        runTest {
            val database = repzDatabaseBuilder().buildRepzDatabase(Dispatchers.IO)
            try {
                val syncMeta = database.syncMetaDao().get()

                assertEquals(SyncMetaEntity.INITIAL, syncMeta)
                assertTrue(NSFileManager.defaultManager.fileExistsAtPath(databasePath))
            } finally {
                database.close()
            }
        }

    @OptIn(ExperimentalForeignApi::class)
    private fun deleteDatabaseFile() {
        NSFileManager.defaultManager.removeItemAtPath(databasePath, error = null)
    }
}
