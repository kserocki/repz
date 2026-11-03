package com.repz.app.core.database

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

/**
 * A file-backed [RepzDatabase] builder under the app's private database directory
 * ([Context.getDatabasePath]), keyed on the application context so the builder outlives any
 * single `Activity`.
 */
fun repzDatabaseBuilder(context: Context): RoomDatabase.Builder<RepzDatabase> {
    val applicationContext = context.applicationContext
    val databaseFile = applicationContext.getDatabasePath(REPZ_DATABASE_FILE_NAME)
    return Room.databaseBuilder<RepzDatabase>(applicationContext, databaseFile.absolutePath)
}
