package com.repz.app.core.data

import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.account.CurrentUserDao
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/**
 * `core:data`'s one `@Module` (ARCH §9, konsist D3). Every repository is an annotated
 * `@Single class OfflineFirst*Repository` picked up by the scan below; the only provider left is
 * [currentUserDao], a Room-generated DAO accessor (`core:database` is Koin-free, ADR-0008).
 */
@Module
@ComponentScan("com.repz.app.core.data")
class DataModule {
    @Single
    fun currentUserDao(database: RepzDatabase): CurrentUserDao = database.currentUserDao()
}
