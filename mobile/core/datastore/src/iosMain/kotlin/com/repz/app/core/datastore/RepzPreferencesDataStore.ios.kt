package com.repz.app.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * A file-backed preferences [DataStore] under the app's Application Support directory (created if
 * missing, the same directory as `repz.db`), never `Documents`, which is user-visible in the Files
 * app.
 */
fun repzPreferencesDataStore(ioDispatcher: CoroutineDispatcher): DataStore<Preferences> =
    createRepzPreferencesDataStore(ioDispatcher) { "${applicationSupportDirectoryPath()}/$REPZ_PREFERENCES_FILE_NAME" }

/** Internal so the iOS test can locate and clean up the same preferences file. */
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
