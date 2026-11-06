package com.repz.app.core.sync.engine

import com.repz.app.core.domain.sync.SyncFailureReason
import com.repz.app.core.network.result.NetworkError

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_PAYLOAD_TOO_LARGE = 413
private val SERVER_ERROR_STATUSES = 500..599

/** §3.5's error table. A 413 is handled by [OutboxPusher] itself before this is ever consulted for one. */
internal fun NetworkError.toSyncOutcome(): SyncOutcome =
    when (this) {
        is NetworkError.Offline, is NetworkError.Timeout -> SyncOutcome.RetryLater(reason = null)
        is NetworkError.SessionExpired -> SyncOutcome.SkippedSignedOut
        is NetworkError.MalformedResponse -> SyncOutcome.Failed(SyncFailureReason.IncompatibleData)
        is NetworkError.Api -> toSyncOutcome()
    }

private fun NetworkError.Api.toSyncOutcome(): SyncOutcome =
    when {
        status == HTTP_UNAUTHORIZED -> SyncOutcome.SkippedSignedOut
        status in SERVER_ERROR_STATUSES -> SyncOutcome.RetryLater(SyncFailureReason.ServerUnavailable)
        else -> SyncOutcome.Failed(SyncFailureReason.Unexpected)
    }

internal fun NetworkError.isPayloadTooLarge(): Boolean = this is NetworkError.Api && status == HTTP_PAYLOAD_TOO_LARGE
