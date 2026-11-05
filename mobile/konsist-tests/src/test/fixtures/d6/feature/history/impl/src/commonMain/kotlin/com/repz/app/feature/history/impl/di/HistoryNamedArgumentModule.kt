package com.repz.app.feature.history.impl.di

import com.repz.app.feature.history.impl.domain.HistoryDependency
import com.repz.app.feature.history.impl.domain.HistoryRepository
import com.repz.app.feature.history.impl.domain.HistoryRepositoryImpl
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class HistoryNamedArgumentModule {
    // A named-argument forward (`dependency = dependency`) is still a pure forward: D6 must see
    // through the parameter label, not just an unlabelled positional argument.
    @Single
    fun historyRepositoryNamedArgument(dependency: HistoryDependency): HistoryRepository =
        HistoryRepositoryImpl(dependency = dependency)
}
