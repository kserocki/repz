package com.repz.app.core.sync.schedule

import com.repz.app.core.domain.sync.SyncScheduler

/** Constructors are side-effect free (graph tests resolve them). */
interface BackgroundSyncScheduler : SyncScheduler {
    /**
     * Synchronous; called once per process from `startRepzSync()` (iOS: registers the BGTask
     * handler and starts the network path monitor).
     */
    fun start()

    fun schedulePeriodic()

    fun cancelAll()
}
