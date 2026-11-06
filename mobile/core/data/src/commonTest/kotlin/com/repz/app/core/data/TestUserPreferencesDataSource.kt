package com.repz.app.core.data

import com.repz.app.core.datastore.UserPreferencesDataSource
import com.repz.app.core.datastore.createRepzPreferencesDataStore
import kotlinx.coroutines.Dispatchers
import okio.FileSystem

// Konsist I1 forbids Uuid.random in tests; a plain, process-wide counter gives every call its own
// file instead, since DataStore allows only one active instance per file (core:datastore §3.6 pitfall).
private var preferencesTestFileCounter = 0

/** A real, file-backed [UserPreferencesDataSource] under a fresh temp file, shared by `core:data`'s tests. */
internal fun newTestUserPreferencesDataSource(): UserPreferencesDataSource {
    val path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "repz-core-data-test-preferences-${preferencesTestFileCounter++}.preferences_pb"
    FileSystem.SYSTEM.delete(path, mustExist = false)
    return UserPreferencesDataSource(createRepzPreferencesDataStore(Dispatchers.Default) { path.toString() })
}
