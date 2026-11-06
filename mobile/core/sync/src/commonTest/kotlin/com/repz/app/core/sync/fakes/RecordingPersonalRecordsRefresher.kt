package com.repz.app.core.sync.fakes

import com.repz.app.core.sync.record.PersonalRecordsRefresher
import kotlin.uuid.Uuid

internal class RecordingPersonalRecordsRefresher : PersonalRecordsRefresher {
    var refreshAllCallCount = 0
        private set
    val refreshForExercisesCalls = mutableListOf<Set<Uuid>>()

    /** Lets a test observe other state (e.g. `sync_meta`) exactly at the moment [refreshAll] runs. */
    var onRefreshAll: suspend () -> Unit = {}

    override suspend fun refreshAll() {
        refreshAllCallCount++
        onRefreshAll()
    }

    override suspend fun refreshForExercises(exerciseIds: Set<Uuid>) {
        refreshForExercisesCalls += exerciseIds
    }
}
