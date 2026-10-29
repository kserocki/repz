package com.repz.app.core.database

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.repz.app.core.database.probe.ProbeDao
import com.repz.app.core.database.probe.ProbeEntity

// TODO(REP-24): replace the probe schema with the real one.
@Database(entities = [ProbeEntity::class], version = 1)
@ConstructedBy(RepzDatabaseConstructor::class)
abstract class RepzDatabase : RoomDatabase() {
    abstract fun probeDao(): ProbeDao
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
