package com.repz.app.shared.di

import com.repz.app.core.common.coroutines.AppDispatchers
import com.repz.app.core.common.id.IdGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatformTools
import org.koin.plugin.module.dsl.startKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.time.Clock

/**
 * Regression test: `CommonModule`'s bindings resolve from the real `RepzApp` graph, not a stub.
 * `startKoin<RepzApp>()` is called directly (rather than through [startRepzKoin]) so the Koin
 * compiler plugin's compile-time graph check can verify every `get<T>()` below against it.
 */
class CommonModuleTest {
    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun givenRepzApp_whenStarted_thenClockIsSystemClock() {
        startKoin<RepzApp>()
        val koin = KoinPlatformTools.defaultContext().get()

        assertSame(Clock.System, koin.get<Clock>())
    }

    @Test
    fun givenRepzApp_whenStarted_thenIdGeneratorProducesIds() {
        startKoin<RepzApp>()
        val idGenerator = KoinPlatformTools.defaultContext().get().get<IdGenerator>()

        assertNotEquals(idGenerator.newId(), idGenerator.newId())
    }

    @Test
    fun givenRepzApp_whenStarted_thenAppDispatchersResolve() {
        startKoin<RepzApp>()
        val dispatchers = KoinPlatformTools.defaultContext().get().get<AppDispatchers>()

        assertSame(Dispatchers.Default, dispatchers.default)
        assertSame(Dispatchers.IO, dispatchers.io)
    }
}
