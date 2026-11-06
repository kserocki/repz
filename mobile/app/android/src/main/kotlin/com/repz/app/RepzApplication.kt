package com.repz.app

import android.app.Application
import androidx.work.Configuration
import com.repz.app.shared.di.repzWorkManagerConfiguration
import com.repz.app.shared.di.startRepzKoin
import com.repz.app.shared.di.startRepzSync
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication

/**
 * Starts the shared Koin graph exactly once per process, before any Compose code runs, then starts
 * the sync engine's background triggers (ARCH §5.6/§9). `Configuration.Provider` replaces
 * WorkManager's default no-arg reflection with the Koin-built `SyncWorkerFactory`; the manifest
 * removes the default `WorkManagerInitializer` so this lazy configuration is the only one used.
 */
class RepzApplication :
    Application(),
    Configuration.Provider {
    private lateinit var koinApplication: KoinApplication

    override fun onCreate() {
        super.onCreate()
        koinApplication =
            startRepzKoin {
                androidContext(this@RepzApplication)
            }
        startRepzSync(koinApplication)
    }

    override val workManagerConfiguration: Configuration
        get() = repzWorkManagerConfiguration(koinApplication)
}
