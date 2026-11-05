package com.repz.app.core.database

import androidx.room3.ColumnTypeConverters
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.repz.app.core.database.account.CurrentUserDao
import com.repz.app.core.database.account.CurrentUserEntity
import com.repz.app.core.database.converter.RepzTypeConverters
import com.repz.app.core.database.exercise.ExerciseDao
import com.repz.app.core.database.exercise.ExerciseEntity
import com.repz.app.core.database.exercise.ExerciseRawDao
import com.repz.app.core.database.exercise.ExerciseTranslationEntity
import com.repz.app.core.database.record.PersonalRecordDao
import com.repz.app.core.database.record.PersonalRecordEntity
import com.repz.app.core.database.sync.OutboxDao
import com.repz.app.core.database.sync.OutboxEntryEntity
import com.repz.app.core.database.sync.SyncMetaDao
import com.repz.app.core.database.sync.SyncMetaEntity
import com.repz.app.core.database.sync.SyncRejectionDao
import com.repz.app.core.database.sync.SyncRejectionEntity
import com.repz.app.core.database.template.TemplateDao
import com.repz.app.core.database.template.TemplateExerciseEntity
import com.repz.app.core.database.template.TemplateRawDao
import com.repz.app.core.database.template.WorkoutTemplateEntity
import com.repz.app.core.database.workout.WorkoutDao
import com.repz.app.core.database.workout.WorkoutEntity
import com.repz.app.core.database.workout.WorkoutExerciseEntity
import com.repz.app.core.database.workout.WorkoutRawDao
import com.repz.app.core.database.workout.WorkoutSetEntity

/** The file name every platform builder stores [RepzDatabase] under. */
const val REPZ_DATABASE_FILE_NAME = "repz.db"

@Database(
    entities = [
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSetEntity::class,
        WorkoutTemplateEntity::class,
        TemplateExerciseEntity::class,
        ExerciseEntity::class,
        ExerciseTranslationEntity::class,
        PersonalRecordEntity::class,
        OutboxEntryEntity::class,
        SyncRejectionEntity::class,
        SyncMetaEntity::class,
        CurrentUserEntity::class,
    ],
    version = 1,
)
@ColumnTypeConverters(RepzTypeConverters::class)
@ConstructedBy(RepzDatabaseConstructor::class)
abstract class RepzDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    abstract fun templateDao(): TemplateDao

    abstract fun exerciseDao(): ExerciseDao

    abstract fun personalRecordDao(): PersonalRecordDao

    abstract fun outboxDao(): OutboxDao

    abstract fun syncRejectionDao(): SyncRejectionDao

    abstract fun syncMetaDao(): SyncMetaDao

    abstract fun currentUserDao(): CurrentUserDao

    /** Backs [WorkoutDao]'s multi-step transactions; not part of this database's public surface. */
    internal abstract fun workoutRawDao(): WorkoutRawDao

    /** Backs [TemplateDao]'s multi-step transactions; not part of this database's public surface. */
    internal abstract fun templateRawDao(): TemplateRawDao

    /** Backs [ExerciseDao]'s multi-step transactions; not part of this database's public surface. */
    internal abstract fun exerciseRawDao(): ExerciseRawDao
}

/**
 * KSP generates the `actual` for each target (`kspAndroid`, `kspIosArm64`,
 * `kspIosSimulatorArm64`); this `expect` has no body by design, per Room's documented
 * multiplatform pattern.
 */
@Suppress("KotlinNoActualForExpect")
expect object RepzDatabaseConstructor : RoomDatabaseConstructor<RepzDatabase> {
    override fun initialize(): RepzDatabase
}
