package com.repz.app.core.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * A file-backed [RepzDatabase] builder under the app's Application Support directory (created if
 * missing), never `Documents`, which is user-visible in the Files app.
 */
@OptIn(ExperimentalForeignApi::class)
fun repzDatabaseBuilder(): RoomDatabase.Builder<RepzDatabase> =
    Room.databaseBuilder<RepzDatabase>(name = "${applicationSupportDirectoryPath()}/$REPZ_DATABASE_FILE_NAME")

/** Internal so the iOS test can locate and clean up the same database file. */
@OptIn(ExperimentalForeignApi::class)
internal fun applicationSupportDirectoryPath(): String {
    val applicationSupportDirectory =
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSApplicationSupportDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
    return requireNotNull(applicationSupportDirectory?.path) {
        "Could not resolve the Application Support directory."
    }
}
