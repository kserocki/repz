package com.repz.app.core.sync.record

import kotlin.uuid.Uuid

/**
 * A `core:sync`-owned port into the domain layer's personal-record recompute use cases, so the
 * engine stays testable without depending on them directly. `app:shared` adapts those use cases
 * to this interface.
 */
interface PersonalRecordsRefresher {
    suspend fun refreshAll()

    suspend fun refreshForExercises(exerciseIds: Set<Uuid>)
}
