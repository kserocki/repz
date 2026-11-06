package com.repz.app.core.domain.sync

import kotlin.time.Instant

/** What `SyncMonitor.status` reports (ARCH §5.6); failure reasons carry no user-facing copy. */
sealed interface SyncStatus {
    data class Idle(
        val lastSyncedAt: Instant?,
    ) : SyncStatus

    data object Syncing : SyncStatus

    data class Offline(
        val pendingCount: Int,
    ) : SyncStatus

    data class Error(
        val reason: SyncFailureReason,
    ) : SyncStatus
}

enum class SyncFailureReason {
    ServerUnavailable,
    IncompatibleData,
    Unexpected,
}
