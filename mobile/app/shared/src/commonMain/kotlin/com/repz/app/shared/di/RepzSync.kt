package com.repz.app.shared.di

import com.repz.app.core.sync.schedule.SyncCoordinator
import org.koin.core.KoinApplication

/**
 * Starts the sync engine's background triggers for the app's lifetime (ARCHITECTURE §5.6/§5.7):
 * the platform scheduler once, then the session-driven periodic schedule and logout cancellation
 * ([SyncCoordinator.start]).
 *
 * Called once per process, right after [startRepzKoin] returns, from `RepzApplication.onCreate`
 * (Android) and `RepzIOSApp.init` (iOS) — **never** from [startRepzKoin] itself, so `SyncGraphTest`
 * and every other Koin graph test can resolve the graph without starting real background work
 * (WorkManager enqueues, `BGTaskScheduler` registration).
 */
fun startRepzSync(koinApplication: KoinApplication) {
    koinApplication.koin.get<SyncCoordinator>().start()
}
