package com.repz.app.feature.history.impl.di

import com.repz.app.feature.history.impl.domain.HistoryDependency
import com.repz.app.feature.history.impl.domain.HistoryRepository
import com.repz.app.feature.history.impl.domain.HistoryRepositoryImpl
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class HistoryBlockBodyModule {
    // A block body with an explicit `return` is still a pure forward: D6 must see through
    // `{ return HistoryRepositoryImpl(dependency) }`, not just the expression-body form.
    @Single
    fun historyRepositoryBlockBody(dependency: HistoryDependency): HistoryRepository {
        return HistoryRepositoryImpl(dependency)
    }
}
