package com.repz.app.core.data

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/** A deterministic, mutable [Clock] for tests (AGENTS.md §4: production code injects a [Clock] instead of reading the system one). */
internal class TestClock(
    private var instant: Instant = Instant.parse("2024-01-01T00:00:00Z"),
) : Clock {
    override fun now(): Instant = instant

    fun advanceBy(duration: Duration) {
        instant += duration
    }
}
