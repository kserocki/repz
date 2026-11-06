package com.repz.app.core.sync.handler

import com.repz.app.core.database.exercise.ExerciseDao
import com.repz.app.core.database.sync.SyncEntityType
import com.repz.app.core.database.template.TemplateDao
import com.repz.app.core.database.workout.WorkoutDao
import com.repz.app.core.network.dto.SyncTypeDto
import com.repz.app.core.sync.exercise.ExerciseSyncHandler
import com.repz.app.core.sync.template.TemplateSyncHandler
import com.repz.app.core.sync.workout.WorkoutSyncHandler
import org.koin.core.annotation.Single

/**
 * Every syncable entity's handler, explicit (ADR-0027): adding a type means adding a handler
 * class plus one line here, and [com.repz.app.core.sync.engine.SyncEngine] never names an entity
 * type. The three handlers are plain classes, not Koin definitions (they bind nothing, so Konsist
 * D5 sees nothing); this class builds them itself from the DAOs it is given.
 */
@Single
class SyncHandlerRegistry(
    workoutDao: WorkoutDao,
    templateDao: TemplateDao,
    exerciseDao: ExerciseDao,
) {
    private val handlers: List<SyncHandler> =
        listOf(
            WorkoutSyncHandler(workoutDao),
            TemplateSyncHandler(templateDao),
            ExerciseSyncHandler(exerciseDao),
        )

    fun all(): List<SyncHandler> = handlers

    fun forEntityType(entityType: SyncEntityType): SyncHandler = handlers.first { it.entityType == entityType }

    fun forWireType(wireType: SyncTypeDto): SyncHandler = handlers.first { it.wireType == wireType }
}
