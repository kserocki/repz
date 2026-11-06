package com.repz.app.core.data

import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.account.CurrentUserDao
import com.repz.app.core.database.exercise.ExerciseDao
import com.repz.app.core.database.record.PersonalRecordDao
import com.repz.app.core.database.sync.OutboxDao
import com.repz.app.core.database.sync.SyncMetaDao
import com.repz.app.core.database.sync.SyncRejectionDao
import com.repz.app.core.database.template.TemplateDao
import com.repz.app.core.database.workout.WorkoutDao
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/**
 * `core:data`'s one `@Module` (ARCH §9, konsist D3). Every repository is an annotated
 * `@Single class OfflineFirst*Repository` picked up by the scan below; the providers left are the
 * Room-generated DAO accessors (`core:database` is Koin-free, ADR-0008). All eight DAOs are
 * provided here, including [syncMetaDao]/[syncRejectionDao] that this module's own repositories
 * don't consume, so the future `core:sync` module never declares a DAO provider of its own (D5).
 */
@Module
@ComponentScan("com.repz.app.core.data")
class DataModule {
    @Single
    fun currentUserDao(database: RepzDatabase): CurrentUserDao = database.currentUserDao()

    @Single
    fun workoutDao(database: RepzDatabase): WorkoutDao = database.workoutDao()

    @Single
    fun templateDao(database: RepzDatabase): TemplateDao = database.templateDao()

    @Single
    fun exerciseDao(database: RepzDatabase): ExerciseDao = database.exerciseDao()

    @Single
    fun personalRecordDao(database: RepzDatabase): PersonalRecordDao = database.personalRecordDao()

    @Single
    fun outboxDao(database: RepzDatabase): OutboxDao = database.outboxDao()

    @Single
    fun syncMetaDao(database: RepzDatabase): SyncMetaDao = database.syncMetaDao()

    @Single
    fun syncRejectionDao(database: RepzDatabase): SyncRejectionDao = database.syncRejectionDao()
}
