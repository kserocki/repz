package com.repz.app.core.sync.schedule

import androidx.work.ListenableWorker
import com.repz.app.core.sync.engine.SyncOutcome

/**
 * The outcome -> [ListenableWorker.Result] table (plan §3.8): only [SyncOutcome.RetryLater] asks
 * WorkManager to retry with its exponential backoff; [SyncOutcome.Failed] waits for the next
 * trigger instead.
 */
internal fun SyncOutcome.toWorkResult(): ListenableWorker.Result =
    when (this) {
        SyncOutcome.Completed, SyncOutcome.SkippedSignedOut, SyncOutcome.Stopped -> ListenableWorker.Result.success()
        is SyncOutcome.RetryLater -> ListenableWorker.Result.retry()
        is SyncOutcome.Failed -> ListenableWorker.Result.failure()
    }
