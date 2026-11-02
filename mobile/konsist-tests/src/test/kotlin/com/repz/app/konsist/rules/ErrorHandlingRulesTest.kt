package com.repz.app.konsist.rules

import com.repz.app.konsist.support.fixtureTextFiles
import com.repz.app.konsist.support.realTextFiles
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFails

private const val RUN_CATCHING_MESSAGE = "E1: no runCatching/.runCatching("
private const val UNSAFE_CATCH_MESSAGE = "E1: a catch of Throwable/Exception/RuntimeException/IllegalStateException/CancellationException"

class ErrorHandlingRulesTest {
    @Test
    fun given_project_whenE1Checked_thenNoViolations() {
        checkE1CancellationSafety(realTextFiles())
    }

    @Test
    fun given_e1RunCatchingTrailingLambdaFixture_whenChecked_thenFailsWithE1() {
        val error = assertFails { checkE1CancellationSafety(fixtureTextFiles("e1-run-catching-trailing-lambda")) }

        assertContains(error.message.orEmpty(), RUN_CATCHING_MESSAGE)
    }

    @Test
    fun given_e1RunCatchingExtensionFixture_whenChecked_thenFailsWithE1() {
        val error = assertFails { checkE1CancellationSafety(fixtureTextFiles("e1-run-catching-extension")) }

        assertContains(error.message.orEmpty(), RUN_CATCHING_MESSAGE)
    }

    @Test
    fun given_e1CatchThrowableFixture_whenChecked_thenFailsWithE1() {
        val error = assertFails { checkE1CancellationSafety(fixtureTextFiles("e1-catch-throwable")) }

        assertContains(error.message.orEmpty(), UNSAFE_CATCH_MESSAGE)
    }

    @Test
    fun given_e1CatchExceptionFixture_whenChecked_thenFailsWithE1() {
        val error = assertFails { checkE1CancellationSafety(fixtureTextFiles("e1-catch-exception")) }

        assertContains(error.message.orEmpty(), UNSAFE_CATCH_MESSAGE)
    }

    @Test
    fun given_e1CatchRuntimeExceptionFixture_whenChecked_thenFailsWithE1() {
        val error = assertFails { checkE1CancellationSafety(fixtureTextFiles("e1-catch-runtime-exception")) }

        assertContains(error.message.orEmpty(), UNSAFE_CATCH_MESSAGE)
    }

    @Test
    fun given_e1CatchIllegalStateExceptionFixture_whenChecked_thenFailsWithE1() {
        val error = assertFails { checkE1CancellationSafety(fixtureTextFiles("e1-catch-illegal-state-exception")) }

        assertContains(error.message.orEmpty(), UNSAFE_CATCH_MESSAGE)
    }

    @Test
    fun given_e1CatchCancellationExceptionFixture_whenChecked_thenFailsWithE1() {
        val error = assertFails { checkE1CancellationSafety(fixtureTextFiles("e1-catch-cancellation-exception")) }

        assertContains(error.message.orEmpty(), UNSAFE_CATCH_MESSAGE)
    }

    @Test
    fun given_e1CatchRethrowsDifferentExceptionFixture_whenChecked_thenFailsWithE1() {
        val error = assertFails { checkE1CancellationSafety(fixtureTextFiles("e1-catch-rethrows-different-exception")) }

        assertContains(error.message.orEmpty(), UNSAFE_CATCH_MESSAGE)
    }

    @Test
    fun given_e1AllowedCatchWithRethrowIfCancellationFixture_whenChecked_thenNoViolations() {
        checkE1CancellationSafety(fixtureTextFiles("e1-allowed-catch-with-rethrow-if-cancellation"))
    }

    @Test
    fun given_e1AllowedCatchWithThrowFixture_whenChecked_thenNoViolations() {
        checkE1CancellationSafety(fixtureTextFiles("e1-allowed-catch-with-throw"))
    }

    @Test
    fun given_e1AllowedCatchNarrowTypeFixture_whenChecked_thenNoViolations() {
        checkE1CancellationSafety(fixtureTextFiles("e1-allowed-catch-narrow-type"))
    }

    @Test
    fun given_e1AllowedTestSourceRunCatchingFixture_whenChecked_thenNoViolations() {
        checkE1CancellationSafety(fixtureTextFiles("e1-allowed-test-source-run-catching"))
    }

    @Test
    fun given_e1AllowedTestSourceCatchExceptionFixture_whenChecked_thenNoViolations() {
        checkE1CancellationSafety(fixtureTextFiles("e1-allowed-test-source-catch-exception"))
    }

    @Test
    fun given_e1AllowedHelperFileFixture_whenChecked_thenNoViolations() {
        checkE1CancellationSafety(fixtureTextFiles("e1-allowed-helper-file"))
    }
}
