package com.repz.app.core.sync.schedule

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/** Unique work name for an on-demand run (ARCH §5.6). */
internal const val SYNC_WORK_NAME = "repz-sync"

/** Unique work name for the hourly background run (ARCH §5.6). */
internal const val SYNC_PERIODIC_WORK_NAME = "repz-sync-periodic"

private val SYNC_BACKOFF_DELAY = 30.seconds
private val SYNC_PERIODIC_INTERVAL = 1.hours

private fun syncConstraints(): Constraints =
    Constraints
        .Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

/** Built without a [WorkManager] instance, so this is host-JVM testable. */
internal fun buildSyncWorkRequest(): OneTimeWorkRequest =
    OneTimeWorkRequestBuilder<SyncWorker>()
        .setConstraints(syncConstraints())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, SYNC_BACKOFF_DELAY.toJavaDuration())
        .build()

internal fun buildPeriodicSyncWorkRequest(): PeriodicWorkRequest =
    PeriodicWorkRequestBuilder<SyncWorker>(SYNC_PERIODIC_INTERVAL.toJavaDuration())
        .setConstraints(syncConstraints())
        .build()

/**
 * WorkManager-backed [BackgroundSyncScheduler] (ARCH §5.6). [WorkManager.getInstance] is resolved
 * lazily, on first use, so constructing this class in a host-JVM Koin graph test never initialises
 * WorkManager.
 */
class AndroidSyncScheduler(
    private val context: Context,
) : BackgroundSyncScheduler {
    private val workManager by lazy { WorkManager.getInstance(context) }

    override fun requestSync() {
        workManager.enqueueUniqueWork(SYNC_WORK_NAME, ExistingWorkPolicy.KEEP, buildSyncWorkRequest())
    }

    /** Android needs no startup registration (unlike iOS's BGTask handler); nothing to do here. */
    override fun start() = Unit

    override fun schedulePeriodic() {
        workManager.enqueueUniquePeriodicWork(
            SYNC_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            buildPeriodicSyncWorkRequest(),
        )
    }

    override fun cancelAll() {
        workManager.cancelUniqueWork(SYNC_WORK_NAME)
        workManager.cancelUniqueWork(SYNC_PERIODIC_WORK_NAME)
    }
}
