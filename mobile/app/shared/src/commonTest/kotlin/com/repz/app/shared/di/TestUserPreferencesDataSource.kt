package com.repz.app.shared.di

import com.repz.app.core.datastore.UserPreferencesDataSource
import com.repz.app.core.datastore.createRepzPreferencesDataStore
import kotlinx.coroutines.Dispatchers
import okio.FileSystem

// Konsist I1 forbids Uuid.random in tests; a plain, process-wide counter gives every Koin start its
// own file instead, since DataStore allows only one active instance per file (core:datastore §3.6).
private var preferencesTestFileCounter = 0

/**
 * A real, file-backed [UserPreferencesDataSource] under a fresh temp file, for
 * [platformTestDeclaration]: the production `PlatformModule.userPreferencesDataSource` needs a real
 * `Context`/Application Support directory that the host test/simulator test process can't supply.
 */
internal fun testUserPreferencesDataSource(): UserPreferencesDataSource {
    val path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "repz-app-shared-test-preferences-${preferencesTestFileCounter++}.preferences_pb"
    FileSystem.SYSTEM.delete(path, mustExist = false)
    return UserPreferencesDataSource(createRepzPreferencesDataStore(Dispatchers.Default) { path.toString() })
}
