package com.repz.app.core.sync

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/** A [Clock] a test can advance by hand instead of reading real wall-clock time. */
internal class MutableClock(
    private var current: Instant,
) : Clock {
    override fun now(): Instant = current

    fun advanceBy(duration: Duration) {
        current += duration
    }
}
