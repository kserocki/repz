package com.repz.app.core.sync.schedule

import androidx.work.BackoffPolicy
import androidx.work.NetworkType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/**
 * Builds the [androidx.work.WorkRequest]s directly and reads their [androidx.work.impl.model.WorkSpec],
 * with no [androidx.work.WorkManager] instance involved, so these run on the host JVM.
 */
class AndroidSyncWorkRequestsTest {
    @Test
    fun givenTheOneTimeSyncRequest_whenBuilt_thenItRunsSyncWorkerAndRequiresConnectivity() {
        val workSpec = buildSyncWorkRequest().workSpec

        assertEquals(SyncWorker::class.java.name, workSpec.workerClassName)
        assertEquals(NetworkType.CONNECTED, workSpec.constraints.requiredNetworkType)
    }

    @Test
    fun givenTheOneTimeSyncRequest_whenBuilt_thenItBacksOffExponentiallyFrom30Seconds() {
        val workSpec = buildSyncWorkRequest().workSpec

        assertEquals(BackoffPolicy.EXPONENTIAL, workSpec.backoffPolicy)
        assertEquals(SYNC_BACKOFF_DELAY_MILLIS, workSpec.backoffDelayDuration)
    }

    @Test
    fun givenThePeriodicSyncRequest_whenBuilt_thenItRunsSyncWorkerAndRequiresConnectivity() {
        val workSpec = buildPeriodicSyncWorkRequest().workSpec

        assertEquals(SyncWorker::class.java.name, workSpec.workerClassName)
        assertEquals(NetworkType.CONNECTED, workSpec.constraints.requiredNetworkType)
    }

    @Test
    fun givenThePeriodicSyncRequest_whenBuilt_thenItRunsEveryHour() {
        val workSpec = buildPeriodicSyncWorkRequest().workSpec

        assertEquals(SYNC_PERIODIC_INTERVAL_MILLIS, workSpec.intervalDuration)
    }

    private companion object {
        val SYNC_BACKOFF_DELAY_MILLIS = 30.seconds.inWholeMilliseconds
        val SYNC_PERIODIC_INTERVAL_MILLIS = 1.hours.inWholeMilliseconds
    }
}
