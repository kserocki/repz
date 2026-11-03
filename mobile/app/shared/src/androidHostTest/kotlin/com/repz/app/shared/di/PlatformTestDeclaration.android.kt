package com.repz.app.shared.di

import android.app.Application
import android.content.Context
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * `app:shared` has no `koin-android` dependency (only `app:android` does, via `androidContext()`
 * in production), so the host test binds a plain [Application] instance directly instead.
 */
internal actual val platformTestDeclaration: KoinAppDeclaration = {
    modules(module { single<Context> { Application() } })
}
