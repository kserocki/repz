package com.repz.app.shared.di

import androidx.work.Configuration
import com.repz.app.core.sync.engine.SyncEngine
import com.repz.app.core.sync.schedule.SyncWorkerFactory
import org.koin.core.KoinApplication

/**
 * WorkManager's manual-initialization [Configuration] (ARCH §5.6/§9): a [SyncWorkerFactory] wired
 * to the Koin-resolved [SyncEngine], so `SyncWorker` is built with its real dependencies instead of
 * WorkManager's default no-arg reflection. [RepzApplication][com.repz.app.RepzApplication] (the only
 * caller) implements `Configuration.Provider` after the manifest removes the default
 * `WorkManagerInitializer`.
 */
fun repzWorkManagerConfiguration(koinApplication: KoinApplication): Configuration {
    val syncEngine = koinApplication.koin.get<SyncEngine>()
    return Configuration
        .Builder()
        .setWorkerFactory(SyncWorkerFactory(syncEngine::sync))
        .build()
}
