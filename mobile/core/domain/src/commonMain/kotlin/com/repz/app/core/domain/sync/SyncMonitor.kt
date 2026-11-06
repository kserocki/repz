package com.repz.app.core.domain.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** What features read to show sync status and the restore progress; implemented by `core:sync`'s `SyncStatusTracker`. */
interface SyncMonitor {
    val status: StateFlow<SyncStatus>
    val restoreState: Flow<RestoreState>
}
