package com.repz.app.shared.di

import androidx.room3.Room
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.buildRepzDatabase
import com.repz.app.core.datastore.UserPreferencesDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * [RepzDatabase] is overridden with an in-memory instance (Koin's default `allowOverride`) so the
 * graph tests never touch the real Application Support file; `core:database`'s own
 * `RepzDatabaseBuilderIosTest` already proves the file-backed builder works in this same process.
 * [UserPreferencesDataSource] is overridden the same way, with a real DataStore under its own temp
 * file: `core:datastore`'s own `RepzPreferencesDataStoreIosTest` already proves the file-backed
 * builder works here, and each Koin start still needs its own file (§3.6).
 */
internal actual val platformTestDeclaration: KoinAppDeclaration = {
    modules(
        module {
            single<RepzDatabase> { Room.inMemoryDatabaseBuilder<RepzDatabase>().buildRepzDatabase(Dispatchers.IO) }
            single<UserPreferencesDataSource> { testUserPreferencesDataSource() }
        },
    )
}
