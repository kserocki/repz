package com.repz.app.core.domain.sync

/** What `SyncMonitor.restoreState` reports while the first pull after login is still running. */
sealed interface RestoreState {
    data object Completed : RestoreState

    /** [progress] is null until the first pull page of the current run arrives (indeterminate loader). */
    data class InProgress(
        val progress: RestoreProgress?,
    ) : RestoreState
}

data class RestoreProgress(
    val restoredWorkouts: Int,
    val totalWorkouts: Int,
    val restoredChanges: Int,
    val totalChanges: Int,
)
