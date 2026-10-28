package com.repz.app.core.common

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Proves `commonTest` actually compiles and runs Kotlin on the Android host test and the iOS
 * simulator test. Without any source, every compile task is NO-SOURCE and a broken toolchain
 * would stay hidden. REP-23 replaces this with real tests and deletes this one.
 */
class BuildSmokeTest {
    @Test
    fun givenScaffold_whenCommonTestRuns_thenKotlinTestIsAvailable() {
        assertTrue(1 + 1 == 2)
    }
}
