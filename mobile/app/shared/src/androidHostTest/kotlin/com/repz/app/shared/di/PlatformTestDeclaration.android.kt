package com.repz.app.shared.di

import android.app.Application
import android.content.Context
import androidx.room3.Room
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.buildRepzDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * `app:shared` has no `koin-android` dependency (only `app:android` does, via `androidContext()`
 * in production), so the host test binds a plain [Application] instance directly instead.
 * [RepzDatabase] is overridden with an in-memory instance (Koin's default `allowOverride`):
 * the android host test process has no real Android `Context` to build a file-backed database
 * against (the unmocked [Application] stub throws on `getDatabasePath`).
 */
internal actual val platformTestDeclaration: KoinAppDeclaration = {
    modules(
        module {
            single<Context> { Application() }
            single<RepzDatabase> { Room.inMemoryDatabaseBuilder<RepzDatabase>().buildRepzDatabase(Dispatchers.IO) }
        },
    )
}
