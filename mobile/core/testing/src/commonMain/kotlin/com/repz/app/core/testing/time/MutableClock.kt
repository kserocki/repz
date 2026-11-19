package com.repz.app.core.testing.time

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * A [Clock] a test can set or advance by hand instead of reading real wall-clock time (I1: a
 * deterministic clock is injected even in tests). The shared replacement for the module-local
 * clock fakes `core:sync`'s `MutableClock` and `core:data`'s `TestClock` carried before this one.
 */
class MutableClock(
    var instant: Instant,
) : Clock {
    override fun now(): Instant = instant

    fun advanceBy(duration: Duration) {
        instant += duration
    }
}
