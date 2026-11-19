package com.repz.app.core.ui.time

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class ElapsedTimeTest {
    @Test
    fun givenZeroSeconds_whenFormatted_thenItIsZeroZero() {
        assertEquals("0:00", formatElapsed(0.seconds))
    }

    @Test
    fun givenFiftyNineSeconds_whenFormatted_thenMinutesStayAtZero() {
        assertEquals("0:59", formatElapsed(59.seconds))
    }

    @Test
    fun givenSixtySeconds_whenFormatted_thenItRollsOverToOneMinute() {
        assertEquals("1:00", formatElapsed(60.seconds))
    }

    @Test
    fun givenThirtySixHundredSeconds_whenFormatted_thenItRollsOverToOneHour() {
        assertEquals("1:00:00", formatElapsed(3_600.seconds))
    }

    @Test
    fun givenThirtySevenTwentyThreeSeconds_whenFormatted_thenHoursMinutesAndSecondsAreAllShown() {
        assertEquals("1:02:03", formatElapsed(3_723.seconds))
    }

    @Test
    fun givenANegativeDuration_whenFormatted_thenItClampsToZero() {
        assertEquals("0:00", formatElapsed((-5).seconds))
    }
}
