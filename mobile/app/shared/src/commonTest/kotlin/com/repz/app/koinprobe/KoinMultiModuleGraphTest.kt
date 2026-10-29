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
 * Regression test (REP-7): a provider function declared in one Gradle module's `@Module` class
 * (`core:testing`) resolves for a consumer declared in another (`app:shared`), on every target
 * including Kotlin/Native. Cross-module `@ComponentScan` drops its hints on Kotlin/Native
 * (koin-compiler-plugin#106); explicit provider functions don't (ADR-0008, option A).
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
