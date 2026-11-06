package com.repz.app.core.sync.status

import com.repz.app.core.database.sync.OutboxDao
import com.repz.app.core.database.sync.SyncMetaDao
import com.repz.app.core.database.sync.SyncMetaEntity
import com.repz.app.core.domain.sync.RestoreProgress
import com.repz.app.core.domain.sync.RestoreState
import com.repz.app.core.domain.sync.SyncMonitor
import com.repz.app.core.domain.sync.SyncStatus
import com.repz.app.core.sync.engine.SyncOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Single

/** [SyncEngine][com.repz.app.core.sync.engine.SyncEngine]'s own view into [status]/[restoreState], called from inside its run. */
@Single
class SyncStatusTracker(
    outboxDao: OutboxDao,
    syncMetaDao: SyncMetaDao,
    applicationScope: CoroutineScope,
) : SyncMonitor {
    private data class RunState(
        val isRunning: Boolean,
        val lastOutcome: SyncOutcome?,
    )

    private val runState = MutableStateFlow(RunState(isRunning = false, lastOutcome = null))
    private val lastRestoreProgress = MutableStateFlow<RestoreProgress?>(null)

    override val status: StateFlow<SyncStatus> =
        combine(runState, outboxDao.observePendingCount(), syncMetaDao.observe()) { run, pendingCount, meta ->
            toStatus(run, pendingCount, meta)
        }.stateIn(applicationScope, SharingStarted.Eagerly, SyncStatus.Idle(null))

    override val restoreState: Flow<RestoreState> =
        combine(syncMetaDao.observe(), lastRestoreProgress) { meta, progress ->
            if (meta.initialPullCompletedAt != null) RestoreState.Completed else RestoreState.InProgress(progress)
        }.distinctUntilChanged()

    fun onRunStarted() {
        runState.update { it.copy(isRunning = true) }
    }

    fun onRunFinished(outcome: SyncOutcome) {
        runState.value = RunState(isRunning = false, lastOutcome = outcome)
    }

    fun onRestoreProgress(progress: RestoreProgress) {
        lastRestoreProgress.value = progress
    }

    private fun toStatus(
        run: RunState,
        pendingCount: Int,
        meta: SyncMetaEntity,
    ): SyncStatus {
        if (run.isRunning) return SyncStatus.Syncing
        return when (val outcome = run.lastOutcome) {
            is SyncOutcome.RetryLater -> outcome.reason?.let { SyncStatus.Error(it) } ?: SyncStatus.Offline(pendingCount)
            is SyncOutcome.Failed -> SyncStatus.Error(outcome.reason)
            SyncOutcome.Completed, SyncOutcome.SkippedSignedOut, SyncOutcome.Stopped, null -> SyncStatus.Idle(meta.lastSuccessfulSyncAt)
        }
    }
}
