package com.repz.app.core.network.result

import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.UnknownHostException

/**
 * OkHttp's "no route to the server" exceptions, on top of [toCommonTransportFailure]'s Ktor-level
 * timeouts. `java.net.SocketTimeoutException` is listed too even though OkHttp's engine already
 * remaps every one it throws into Ktor's own `io.ktor.client.network.sockets.SocketTimeoutException`
 * (a JVM `typealias` for the same class, so [toCommonTransportFailure] already matches it first);
 * kept explicit here so this mapping reads correctly standalone, per the plan (§3.4).
 */
internal actual fun Throwable.toTransportFailure(): NetworkError? =
    toCommonTransportFailure() ?: when (this) {
        is java.net.SocketTimeoutException -> NetworkError.Timeout
        is UnknownHostException, is ConnectException, is NoRouteToHostException, is SocketException, is IOException -> NetworkError.Offline
        else -> null
    }
