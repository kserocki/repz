package com.repz.app.core.common.coroutines

import kotlinx.coroutines.CancellationException

fun Throwable.rethrowIfCancellation() {
    if (this is CancellationException) throw this
}

inline fun <T> suspendRunCatching(block: () -> T): Result<T> =
    runCatching(block).onFailure { it.rethrowIfCancellation() }

// Proves the file-path exemption holds even for a catch that would otherwise violate E1:
// this is the one file allowed to swallow a caught exception without rethrowing it.
private fun demonstrateExemptCatch() {
    try {
        riskyWork()
    } catch (e: Throwable) {
        logSwallowed(e)
    }
}

private fun riskyWork() {}

private fun logSwallowed(e: Throwable) {}
