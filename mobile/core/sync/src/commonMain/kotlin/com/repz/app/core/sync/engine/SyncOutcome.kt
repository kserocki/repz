package com.repz.app.core.sync.engine

import com.repz.app.core.domain.sync.SyncFailureReason

/** What one [SyncEngine.sync] run ended with. */
sealed interface SyncOutcome {
    data object Completed : SyncOutcome

    /** No signed-in session (incl. the debug signed-in flag, whose real session is signed out). */
    data object SkippedSignedOut : SyncOutcome

    /** [SyncEngine.stop] cancelled the run (logout). */
    data object Stopped : SyncOutcome

    /** Offline, timeout, 5xx: the platform retries with backoff. */
    data class RetryLater(
        val reason: SyncFailureReason?,
    ) : SyncOutcome

    /** Anything else (whole-request 4xx, malformed/unknown vocabulary): no immediate retry; next trigger. */
    data class Failed(
        val reason: SyncFailureReason,
    ) : SyncOutcome
}
