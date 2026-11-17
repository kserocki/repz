package com.repz.app.core.designsystem.component

import com.repz.app.core.designsystem.motion.RepzDurations
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

class RepzSkeletonDelayTest {
    @Test
    fun given_noTimeElapsed_whenComputingRemainingDelay_thenReturnsTheFullDelay() {
        assertEquals(150.milliseconds, remainingSkeletonDelay(elapsed = Duration.ZERO, delay = 150.milliseconds))
    }

    @Test
    fun given_partialTimeElapsed_whenComputingRemainingDelay_thenSubtractsIt() {
        assertEquals(60.milliseconds, remainingSkeletonDelay(elapsed = 90.milliseconds, delay = 150.milliseconds))
    }

    @Test
    fun given_elapsedAtOrPastTheDelay_whenComputingRemainingDelay_thenReturnsZero() {
        assertEquals(Duration.ZERO, remainingSkeletonDelay(elapsed = 150.milliseconds, delay = 150.milliseconds))
        assertEquals(Duration.ZERO, remainingSkeletonDelay(elapsed = 400.milliseconds, delay = 150.milliseconds))
    }

    @Test
    fun given_aFakeClockElapsed_whenComputingRemainingDelay_thenMatchesTheMeasuredElapsed() {
        val timeSource = TestTimeSource()
        val start = timeSource.markNow()
        timeSource += 90.milliseconds

        assertEquals(60.milliseconds, remainingSkeletonDelay(start.elapsedNow(), RepzDurations.skeletonDelay))
    }

    @Test
    fun given_noDelayArgument_whenComputingRemainingDelay_thenDefaultsToTheSkeletonDelayToken() {
        assertEquals(RepzDurations.skeletonDelay, remainingSkeletonDelay(elapsed = Duration.ZERO))
    }
}
