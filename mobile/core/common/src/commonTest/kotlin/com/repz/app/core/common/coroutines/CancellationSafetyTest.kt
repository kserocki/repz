package com.repz.app.core.common.coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

private class BoomException(
    message: String,
) : RuntimeException(message)

class CancellationSafetyTest {
    @Test
    fun givenBlockThatSucceeds_whenSuspendRunCatching_thenResultIsSuccessWithValue() {
        val result = suspendRunCatching { 42 }

        assertEquals(Result.success(42), result)
    }

    @Test
    fun givenBlockThrowsOrdinaryException_whenSuspendRunCatching_thenResultIsFailureWrappingIt() {
        val result = suspendRunCatching { throw BoomException("boom") }

        val failure = result.exceptionOrNull()
        assertIs<BoomException>(failure)
        assertEquals("boom", failure.message)
    }

    @Test
    fun givenBlockThrowsCancellationException_whenSuspendRunCatching_thenItIsRethrownNotWrapped() {
        val error = assertFails { suspendRunCatching { throw CancellationException("cancelled") } }

        assertIs<CancellationException>(error)
        assertEquals("cancelled", error.message)
    }

    @Test
    fun givenCancellationException_whenRethrowIfCancellationCalled_thenItIsThrown() {
        val cancellation = CancellationException("cancelled")

        val error = assertFails { cancellation.rethrowIfCancellation() }

        assertEquals(cancellation, error)
    }

    @Test
    fun givenOrdinaryThrowable_whenRethrowIfCancellationCalled_thenNothingHappens() {
        val ordinary: Throwable = BoomException("boom")

        ordinary.rethrowIfCancellation()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun givenJobCancelledDuringSuspendInBlock_whenSuspendRunCatching_thenCancellationPropagatesNotSwallowed() =
        runTest(UnconfinedTestDispatcher()) {
            var reachedAfterBlock = false

            val job =
                launch {
                    suspendRunCatching { delay(1.seconds) }
                    reachedAfterBlock = true
                }

            job.cancel()
            job.join()

            assertTrue(job.isCancelled)
            assertFalse(reachedAfterBlock)
        }
}
