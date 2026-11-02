package com.repz.app.shared.di

import com.repz.app.core.common.coroutines.AppDispatchers
import com.repz.app.core.common.id.IdGenerator
import com.repz.app.core.common.id.UuidV7IdGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import kotlin.time.Clock

/** The only place `Clock.System` is read (konsist I1): every other definition injects a [Clock]. */
@Module
class CommonModule {
    @Single
    fun clock(): Clock = Clock.System

    @Single
    fun idGenerator(): IdGenerator = UuidV7IdGenerator()

    @Single
    fun appDispatchers(): AppDispatchers = AppDispatchers(default = Dispatchers.Default, io = Dispatchers.IO)
}
