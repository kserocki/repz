package com.repz.app.core.database.account

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CurrentUserDao {
    @Query("SELECT * FROM current_user LIMIT 1")
    abstract fun observe(): Flow<CurrentUserEntity?>

    @Query("DELETE FROM current_user")
    abstract suspend fun clear()

    @Insert
    internal abstract suspend fun insert(entity: CurrentUserEntity)

    /** Replaces the cached row with [entity], keeping exactly one row (e.g. switching accounts). */
    @Transaction
    open suspend fun replace(entity: CurrentUserEntity) {
        clear()
        insert(entity)
    }
}
