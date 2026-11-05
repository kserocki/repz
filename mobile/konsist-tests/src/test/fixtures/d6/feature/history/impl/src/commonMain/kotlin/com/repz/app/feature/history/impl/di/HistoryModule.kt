package com.repz.app.feature.history.impl.di

import com.repz.app.feature.history.impl.domain.HistoryDependency
import com.repz.app.feature.history.impl.domain.HistoryRepository
import com.repz.app.feature.history.impl.domain.HistoryRepositoryImpl
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class HistoryModule {
    // Pure forward: every argument is a bare parameter, so annotating HistoryRepositoryImpl
    // directly would do exactly the same thing - this provider earns its keep from nothing.
    @Single
    fun historyRepository(dependency: HistoryDependency): HistoryRepository = HistoryRepositoryImpl(dependency)
}
