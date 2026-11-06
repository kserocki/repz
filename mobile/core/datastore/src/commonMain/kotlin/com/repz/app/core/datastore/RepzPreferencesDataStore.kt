package com.repz.app.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import okio.Path.Companion.toPath

/** The preferences `DataStore` file name, shared by every platform builder and every test. */
const val REPZ_PREFERENCES_FILE_NAME = "repz.preferences_pb"

/**
 * The one place that builds the preferences `DataStore`; every platform builder
 * ([com.repz.app.core.datastore] `repzPreferencesDataStore`) and every test goes through it.
 */
fun createRepzPreferencesDataStore(
    ioDispatcher: CoroutineDispatcher,
    producePath: () -> String,
): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        scope = CoroutineScope(ioDispatcher + SupervisorJob()),
        produceFile = { producePath().toPath() },
    )
