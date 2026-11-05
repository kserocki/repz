package com.repz.app.koinprobe

import com.repz.app.core.testing.koin.KoinProbeContract
import com.repz.app.core.testing.koin.KoinProbeDependency
import com.repz.app.core.testing.koin.KoinProbeProvidedDependency
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatformTools
import org.koin.plugin.module.dsl.startKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame

/**
 * Regression test for both definition shapes declared in one Gradle module's `@Module` class
 * (`core:testing`) resolving for a consumer declared in another (`app:shared`), on every target
 * including Kotlin/Native (this was broken on iOS before the koin-compiler-plugin version that
 * fixes it, ADR-0008): a scanned, auto-bound class ([KoinProbeDependency] / [KoinProbeContract])
 * and an explicit provider function ([KoinProbeProvidedDependency]).
 */
class KoinMultiModuleGraphTest {
    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun givenProviderInAnotherModule_whenConsumerIsResolved_thenItReceivesTheSingleton() {
        startKoin<KoinProbeApp>()
        val koin = KoinPlatformTools.defaultContext().get()

        val firstConsumer = koin.get<KoinProbeConsumer>()
        val secondConsumer = koin.get<KoinProbeConsumer>()
        val dependency = koin.get<KoinProbeDependency>()

        assertNotSame(firstConsumer, secondConsumer)
        assertSame(dependency, firstConsumer.dependency)
        assertSame(dependency, secondConsumer.dependency)
    }

    @Test
    fun givenScannedClassInAnotherModule_whenResolvedByItsInterface_thenAutoBindingCrossesTheModuleBoundary() {
        startKoin<KoinProbeApp>()
        val koin = KoinPlatformTools.defaultContext().get()

        assertSame(koin.get<KoinProbeDependency>(), koin.get<KoinProbeContract>())
    }

    @Test
    fun givenProviderFunctionInAnotherModule_whenResolved_thenItReceivesTheSingleton() {
        startKoin<KoinProbeApp>()
        val koin = KoinPlatformTools.defaultContext().get()

        assertNotNull(koin.get<KoinProbeProvidedDependency>())
        assertSame(koin.get<KoinProbeProvidedDependency>(), koin.get<KoinProbeConsumer>().provided)
        assertEquals("koin-probe-provided", koin.get<KoinProbeProvidedDependency>().label)
    }
}
