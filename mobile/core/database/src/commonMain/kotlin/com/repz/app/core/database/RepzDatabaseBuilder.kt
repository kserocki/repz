package com.repz.app.core.database

import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.coroutines.CoroutineContext

/**
 * The one place that fixes `RepzDatabase` on the bundled SQLite driver (ADR-0007). Every builder
 * on every platform, and every test, goes through this instead of calling `setDriver` itself.
 */
fun RoomDatabase.Builder<RepzDatabase>.buildRepzDatabase(
    queryCoroutineContext: CoroutineContext,
): RepzDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(queryCoroutineContext)
        .build()
