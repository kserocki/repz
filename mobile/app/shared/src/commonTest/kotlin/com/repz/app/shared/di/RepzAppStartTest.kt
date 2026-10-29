package com.repz.app.shared.di

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertSame
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatformTools

/**
 * Regression test: the real `RepzApp` graph starts on every target, including the iOS
 * simulator, not only compiles.
 */
class RepzAppStartTest {
    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun givenRepzApp_whenStartRepzKoinIsCalled_thenKoinIsRegisteredGlobally() {
        val application = startRepzKoin()

        assertSame(KoinPlatformTools.defaultContext().get(), application.koin)
    }
}
