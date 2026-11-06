package com.repz.app.core.sync.engine

import com.repz.app.core.domain.sync.SyncFailureReason
import com.repz.app.core.network.result.NetworkError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncErrorMappingTest {
    @Test
    fun givenOffline_whenMapped_thenRetryLaterWithNoReason() {
        assertEquals(SyncOutcome.RetryLater(null), NetworkError.Offline.toSyncOutcome())
    }

    @Test
    fun givenTimeout_whenMapped_thenRetryLaterWithNoReason() {
        assertEquals(SyncOutcome.RetryLater(null), NetworkError.Timeout.toSyncOutcome())
    }

    @Test
    fun givenSessionExpired_whenMapped_thenSkippedSignedOut() {
        assertEquals(SyncOutcome.SkippedSignedOut, NetworkError.SessionExpired.toSyncOutcome())
    }

    @Test
    fun given401_whenMapped_thenSkippedSignedOut() {
        assertEquals(SyncOutcome.SkippedSignedOut, NetworkError.Api(401, null).toSyncOutcome())
    }

    @Test
    fun given500_whenMapped_thenRetryLaterWithServerUnavailable() {
        assertEquals(SyncOutcome.RetryLater(SyncFailureReason.ServerUnavailable), NetworkError.Api(500, null).toSyncOutcome())
    }

    @Test
    fun given503_whenMapped_thenRetryLaterWithServerUnavailable() {
        assertEquals(SyncOutcome.RetryLater(SyncFailureReason.ServerUnavailable), NetworkError.Api(503, null).toSyncOutcome())
    }

    @Test
    fun givenAnOther4xx_whenMapped_thenFailedWithUnexpected() {
        assertEquals(SyncOutcome.Failed(SyncFailureReason.Unexpected), NetworkError.Api(409, null).toSyncOutcome())
    }

    @Test
    fun givenMalformedResponse_whenMapped_thenFailedWithIncompatibleData() {
        assertEquals(SyncOutcome.Failed(SyncFailureReason.IncompatibleData), NetworkError.MalformedResponse("bad").toSyncOutcome())
    }

    @Test
    fun given413_whenCheckedForPayloadTooLarge_thenTrue() {
        assertTrue(NetworkError.Api(413, null).isPayloadTooLarge())
    }

    @Test
    fun givenANon413Error_whenCheckedForPayloadTooLarge_thenFalse() {
        assertFalse(NetworkError.Api(400, null).isPayloadTooLarge())
        assertFalse(NetworkError.Offline.isPayloadTooLarge())
    }
}
