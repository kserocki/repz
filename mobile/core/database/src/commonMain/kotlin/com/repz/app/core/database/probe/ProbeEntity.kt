package com.repz.app.core.database.probe

import androidx.room3.Entity
import androidx.room3.PrimaryKey

// TODO(REP-24): replace the probe with the real schema.
@Entity(tableName = "probe")
data class ProbeEntity(
    @PrimaryKey val id: String,
    val label: String,
)
