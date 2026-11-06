package com.repz.app.shared.di

import com.repz.app.core.data.DataModule
import com.repz.app.core.network.NetworkModule
import com.repz.app.core.sync.SyncModule
import com.repz.app.feature.auth.impl.di.AuthModule
import com.repz.app.feature.settings.impl.di.SettingsModule
import org.koin.core.KoinApplication
import org.koin.dsl.KoinAppDeclaration
import org.koin.plugin.module.dsl.startKoin

/**
 * The single Koin entry point (ARCHITECTURE §9). The full dependency graph is verified at compile
 * time wherever this annotation's generated `startKoin<RepzApp>()` call is compiled: here, in
 * `app:shared`'s main sources and its `commonTest` sources.
 */
@org.koin.core.annotation.KoinApplication(
    modules = [
        AppModule::class, AuthModule::class, CommonModule::class, DataModule::class, DomainModule::class,
        NetworkModule::class, PlatformModule::class, SettingsModule::class, SyncModule::class,
    ],
)
object RepzApp

/**
 * Starts the real dependency graph. [platformDeclaration] is where Android/iOS add platform-only
 * bindings (`androidContext(...)`, loggers); it stays a single function, no Swift-specific overload.
 */
fun startRepzKoin(platformDeclaration: KoinAppDeclaration = {}): KoinApplication = startKoin<RepzApp> { platformDeclaration() }
