package com.repz.app.shared

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
class DispatcherModule {
    @Single
    @Named("io")
    fun io(): CoroutineDispatcher = Dispatchers.IO

    @Single
    @Named("default")
    fun default(): CoroutineDispatcher = Dispatchers.Default
}
