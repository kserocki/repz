package com.repz.app.core.database.sync

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlin.time.Instant

/**
 * A singleton row, always [SINGLETON_ID]. [restoreTotalChanges]/[restoreTotalWorkouts] let the
 * restore screen's "N / M" baseline survive an app kill: N is derived from the remaining estimate
 * and this stored total.
 */
@Entity(tableName = "sync_meta")
data class SyncMetaEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "pull_cursor") val pullCursor: Long,
    @ColumnInfo(name = "last_successful_sync_at") val lastSuccessfulSyncAt: Instant?,
    @ColumnInfo(name = "initial_pull_completed_at") val initialPullCompletedAt: Instant?,
    @ColumnInfo(name = "restore_total_changes") val restoreTotalChanges: Int?,
    @ColumnInfo(name = "restore_total_workouts") val restoreTotalWorkouts: Int?,
) {
    companion object {
        const val SINGLETON_ID = 0

        val INITIAL =
            SyncMetaEntity(
                id = SINGLETON_ID,
                pullCursor = 0L,
                lastSuccessfulSyncAt = null,
                initialPullCompletedAt = null,
                restoreTotalChanges = null,
                restoreTotalWorkouts = null,
            )
    }
}
