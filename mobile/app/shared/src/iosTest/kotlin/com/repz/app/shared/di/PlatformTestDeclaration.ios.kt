package com.repz.app.shared.di

import androidx.room3.Room
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.buildRepzDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * [RepzDatabase] is overridden with an in-memory instance (Koin's default `allowOverride`) so the
 * graph tests never touch the real Application Support file; `core:database`'s own
 * `RepzDatabaseBuilderIosTest` already proves the file-backed builder works in this same process.
 */
internal actual val platformTestDeclaration: KoinAppDeclaration = {
    modules(
        module {
            single<RepzDatabase> { Room.inMemoryDatabaseBuilder<RepzDatabase>().buildRepzDatabase(Dispatchers.IO) }
        },
    )
}
