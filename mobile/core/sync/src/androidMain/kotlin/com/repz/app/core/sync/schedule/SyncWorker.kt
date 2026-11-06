package com.repz.app.core.sync.schedule

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.repz.app.core.sync.engine.SyncOutcome

/**
 * Runs one [SyncOutcome]-producing sync under WorkManager. [runSync] is `SyncEngine::sync`, passed
 * in by [SyncWorkerFactory] instead of resolved here, so this class carries no DI framework
 * dependency of its own.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val runSync: suspend () -> SyncOutcome,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): ListenableWorker.Result = runSync().toWorkResult()
}
