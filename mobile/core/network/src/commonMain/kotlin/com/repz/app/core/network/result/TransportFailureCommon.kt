package com.repz.app.core.network.result

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException

/**
 * The platform-independent part of [toTransportFailure]: Ktor's own timeout exceptions, which exist
 * on every engine. Every platform `actual` calls this first, then falls back to its own engine's
 * "no route to the server" exception types. None of these three types is a `CancellationException`
 * in Ktor 3.6 (verified against the `ktor-client-core` sources), so matching them before or after
 * `rethrowIfCancellation()` makes no difference in practice; they're still matched first here as the
 * cheaper, engine-independent check.
 */
internal fun Throwable.toCommonTransportFailure(): NetworkError? =
    when (this) {
        is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException -> NetworkError.Timeout
        else -> null
    }
