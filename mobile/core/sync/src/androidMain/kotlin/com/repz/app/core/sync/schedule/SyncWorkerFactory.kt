package com.repz.app.core.sync.schedule

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.repz.app.core.sync.engine.SyncOutcome

/**
 * Builds [SyncWorker] with the Koin-provided [runSync] (`SyncEngine::sync`), bypassing
 * WorkManager's default no-arg-constructor instantiation (ARCH §5.6/§9). Returns `null` for any
 * other worker class name, so [WorkerFactory.createWorkerWithDefaultFallback] still builds workers
 * this factory doesn't know about.
 */
class SyncWorkerFactory(
    private val runSync: suspend () -> SyncOutcome,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? =
        if (workerClassName == SyncWorker::class.java.name) {
            SyncWorker(appContext, workerParameters, runSync)
        } else {
            null
        }
}
