package com.repz.app.shared.di

import com.repz.app.core.common.coroutines.AppDispatchers
import com.repz.app.core.common.id.IdGenerator
import com.repz.app.core.common.id.UuidV7IdGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * `core:common` stays Koin-free (AGENTS §4), so every binding here is a provider, not a class
 * annotation; each KDoc line below names which of ADR-0008's exempt reasons applies.
 */
@Module
class CommonModule {
    /**
     * Reason 1: a third-party value read from a function. The only place `Clock.System` is read
     * (konsist I1): every other definition injects a [Clock].
     */
    @Single
    fun clock(): Clock = Clock.System

    /** Reason 4: [UuidV7IdGenerator] is declared in `core:common`, which never sees Koin. */
    @Single
    fun idGenerator(): IdGenerator = UuidV7IdGenerator()

    /** Reason 4: [AppDispatchers] is declared in `core:common`, which never sees Koin. */
    @Single
    fun appDispatchers(): AppDispatchers = AppDispatchers(default = Dispatchers.Default, io = Dispatchers.IO)

    /**
     * Reason 1: a third-party [CoroutineScope] built from a `SupervisorJob()`. The app-lifetime
     * scope `core:data`'s repositories share their `stateIn` flows on (ADR-0025).
     */
    @Single
    fun applicationScope(dispatchers: AppDispatchers): CoroutineScope = CoroutineScope(SupervisorJob() + dispatchers.default)
}
