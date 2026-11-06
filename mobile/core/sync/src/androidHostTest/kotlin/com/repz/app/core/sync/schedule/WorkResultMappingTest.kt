package com.repz.app.core.sync.schedule

import androidx.work.ListenableWorker
import com.repz.app.core.domain.sync.SyncFailureReason
import com.repz.app.core.sync.engine.SyncOutcome
import kotlin.test.Test
import kotlin.test.assertEquals

class WorkResultMappingTest {
    @Test
    fun givenCompleted_whenMappedToWorkResult_thenItIsSuccess() {
        assertEquals(ListenableWorker.Result.success(), SyncOutcome.Completed.toWorkResult())
    }

    @Test
    fun givenSkippedSignedOut_whenMappedToWorkResult_thenItIsSuccess() {
        assertEquals(ListenableWorker.Result.success(), SyncOutcome.SkippedSignedOut.toWorkResult())
    }

    @Test
    fun givenStopped_whenMappedToWorkResult_thenItIsSuccess() {
        assertEquals(ListenableWorker.Result.success(), SyncOutcome.Stopped.toWorkResult())
    }

    @Test
    fun givenRetryLaterWithNoReason_whenMappedToWorkResult_thenItIsRetry() {
        assertEquals(ListenableWorker.Result.retry(), SyncOutcome.RetryLater(reason = null).toWorkResult())
    }

    @Test
    fun givenRetryLaterWithAReason_whenMappedToWorkResult_thenItIsRetry() {
        assertEquals(
            ListenableWorker.Result.retry(),
            SyncOutcome.RetryLater(reason = SyncFailureReason.ServerUnavailable).toWorkResult(),
        )
    }

    @Test
    fun givenFailed_whenMappedToWorkResult_thenItIsFailure() {
        assertEquals(
            ListenableWorker.Result.failure(),
            SyncOutcome.Failed(reason = SyncFailureReason.Unexpected).toWorkResult(),
        )
    }
}
