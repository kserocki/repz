package com.repz.app.core.ui.time

import kotlin.time.Duration

/**
 * The live workout header's elapsed timer: "m:ss" under an hour, "h:mm:ss" from an hour on (e.g.
 * "0:12", "34:12", "1:02:03"). A negative [elapsed] clamps to zero. Digits only, not locale-aware:
 * this is a running counter the user reads, never a value they type back in.
 */
fun formatElapsed(elapsed: Duration): String =
    elapsed.coerceAtLeast(Duration.ZERO).toComponents { hours, minutes, seconds, _ ->
        if (hours > 0) {
            "$hours:${minutes.twoDigits()}:${seconds.twoDigits()}"
        } else {
            "$minutes:${seconds.twoDigits()}"
        }
    }

private fun Int.twoDigits(): String = toString().padStart(length = 2, padChar = '0')
