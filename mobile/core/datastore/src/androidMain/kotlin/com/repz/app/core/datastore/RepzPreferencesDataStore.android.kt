package com.repz.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineDispatcher

/**
 * A file-backed preferences [DataStore] under the app's private files directory, keyed on the
 * application context so the builder outlives any single `Activity`.
 */
fun repzPreferencesDataStore(
    context: Context,
    ioDispatcher: CoroutineDispatcher,
): DataStore<Preferences> {
    val applicationContext = context.applicationContext
    return createRepzPreferencesDataStore(ioDispatcher) {
        applicationContext.filesDir.resolve("datastore/$REPZ_PREFERENCES_FILE_NAME").absolutePath
    }
}
