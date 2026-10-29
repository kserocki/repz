package com.repz.app.core.database.probe

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

// TODO(REP-24): replace the probe with the real schema.
@Dao
interface ProbeDao {
    @Upsert
    suspend fun upsert(probe: ProbeEntity)

    @Query("SELECT * FROM probe WHERE id = :id")
    suspend fun getById(id: String): ProbeEntity?

    @Query("SELECT * FROM probe ORDER BY id")
    fun observeAll(): Flow<List<ProbeEntity>>
}
