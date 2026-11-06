package com.repz.app.shared.sync

import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.record.RecalculateAllPersonalRecordsUseCase
import com.repz.app.core.domain.record.RecalculatePersonalRecordsUseCase
import com.repz.app.core.sync.record.PersonalRecordsRefresher
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

/**
 * Adapts `core:sync`'s [PersonalRecordsRefresher] port to the domain layer's recompute use cases
 * (ADR-0027), so `core:sync` stays testable with a recording fake without depending on
 * `core:domain`'s recompute use cases directly. [Uuid] (the wire/engine id) maps to [ExerciseId]
 * here, the one boundary where that conversion belongs.
 *
 * The real graph is cyclic through this class on iOS (`PersonalRecordsRefresherAdapter` → a use
 * case → `WorkoutRepository` → `SyncScheduler` → `SyncEngine` → `PersonalRecordsRefresher` → this
 * class again), because `IosSyncScheduler` needs a `SyncEngine` reference to call back into it. That
 * cycle is broken on the other side, in `PlatformModule.syncScheduler`'s iOS `actual`, by injecting
 * `Lazy<SyncEngine>` there instead: this class stays a plain, eagerly-constructed adapter.
 */
@Single
class PersonalRecordsRefresherAdapter(
    private val recalculateForExercises: RecalculatePersonalRecordsUseCase,
    private val recalculateAll: RecalculateAllPersonalRecordsUseCase,
) : PersonalRecordsRefresher {
    override suspend fun refreshAll() = recalculateAll()

    override suspend fun refreshForExercises(exerciseIds: Set<Uuid>) {
        recalculateForExercises(exerciseIds.map(::ExerciseId).toSet())
    }
}
