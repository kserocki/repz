package com.repz.app.core.common.coroutines

import kotlinx.coroutines.CancellationException

/**
 * Rethrows this [Throwable] if it is a [CancellationException], otherwise does nothing. Coroutine
 * cancellation relies on that exception propagating out of every suspend call it passes through,
 * so a hand-written `catch` block must call this before handling any other exception type
 * (konsist E1).
 */
fun Throwable.rethrowIfCancellation() {
    if (this is CancellationException) throw this
}

/**
 * Like [runCatching], but rethrows [CancellationException] instead of wrapping it in a failed
 * [Result]. A plain `runCatching`/`catch (e: Exception)` swallows coroutine cancellation and
 * breaks structured concurrency, so this is the only form allowed in production code (konsist E1).
 */
inline fun <T> suspendRunCatching(block: () -> T): Result<T> = runCatching(block).onFailure { it.rethrowIfCancellation() }
