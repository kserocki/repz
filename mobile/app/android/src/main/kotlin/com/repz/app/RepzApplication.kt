package com.repz.app

import android.app.Application
import com.repz.app.shared.di.startRepzKoin
import org.koin.android.ext.koin.androidContext

/** Starts the shared Koin graph exactly once per process, before any Compose code runs. */
class RepzApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startRepzKoin {
            androidContext(this@RepzApplication)
        }
    }
}
