package com.repz.app.koinprobe

import com.repz.app.core.testing.koin.KoinProbeDependency
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatformTools
import org.koin.plugin.module.dsl.startKoin

/**
 * Regression test: a provider function declared in one Gradle module's `@Module` class
 * (`core:testing`) resolves for a consumer declared in another (`app:shared`), on every target
 * including Kotlin/Native (see ADR-0008 for why this uses provider functions, not `@ComponentScan`).
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
}
