package com.repz.app.core.data.sync

import com.repz.app.core.domain.sync.SyncScheduler
import org.koin.core.annotation.Single

/** A temporary placeholder: every write already asks for a sync, there is just no engine to run one yet. */
@Single
class NoOpSyncScheduler : SyncScheduler {
    // TODO(REP-26): delete this class when core:sync binds the platform schedulers to SyncScheduler.
    override fun requestSync() = Unit
}
