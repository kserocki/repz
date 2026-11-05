package com.repz.app.core.data.di

import com.repz.app.core.common.id.IdGenerator
import com.repz.app.core.common.id.UuidV7IdGenerator
import com.repz.app.core.database.CurrentUserDao
import com.repz.app.core.database.RepzDatabase
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class DataAccessorsModule {
    // A qualified call (`database.currentUserDao()`), not a bare constructor call of a project
    // class: a Room-generated accessor, not a pass-through (ADR-0008 reason 2).
    @Single
    fun currentUserDao(database: RepzDatabase): CurrentUserDao = database.currentUserDao()

    // UuidV7IdGenerator is declared in core:common, which is Koin-free (ADR-0008 reason 4): it can
    // never carry its own annotation, so this provider is exempt even though it is a bare call.
    @Single
    fun idGenerator(): IdGenerator = UuidV7IdGenerator()
}
