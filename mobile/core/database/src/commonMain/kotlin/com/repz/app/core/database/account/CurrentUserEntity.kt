package com.repz.app.core.database.account

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlin.uuid.Uuid

/**
 * A single-row cache of the signed-in user's profile (ADR-0023 amendment): server-owned, not
 * synced, no outbox. [id] is the real user id, not a fixed singleton marker, so [CurrentUserDao]'s
 * `replace` can tell one account's cached row from another's when the user switches accounts.
 */
@Entity(tableName = "current_user")
data class CurrentUserEntity(
    @PrimaryKey val id: Uuid,
    val email: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "unit_system") val unitSystem: String,
)
