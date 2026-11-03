package com.repz.app.core.network.result

import io.ktor.client.engine.darwin.DarwinHttpRequestException
import kotlinx.io.IOException
import platform.Foundation.NSURLErrorCannotConnectToHost
import platform.Foundation.NSURLErrorCannotFindHost
import platform.Foundation.NSURLErrorDNSLookupFailed
import platform.Foundation.NSURLErrorDataNotAllowed
import platform.Foundation.NSURLErrorInternationalRoamingOff
import platform.Foundation.NSURLErrorNetworkConnectionLost
import platform.Foundation.NSURLErrorNotConnectedToInternet
import platform.Foundation.NSURLErrorTimedOut

/**
 * Darwin wraps every `NSURLSession`-level failure in [DarwinHttpRequestException], whose
 * [NSError][DarwinHttpRequestException.origin] code tells a "no route to the server" failure apart
 * from everything else. `NSURLErrorTimedOut` is matched defensively here even though Ktor's own
 * `handleNSError` already turns it into a [io.ktor.client.network.sockets.SocketTimeoutException]
 * before it ever reaches this function (caught by [toCommonTransportFailure] instead). Any other
 * `kotlinx.io.IOException` (including a [DarwinHttpRequestException] with an unlisted code) is still
 * [NetworkError.Offline], the same breadth as the Android actual's `java.io.IOException` catch-all.
 */
internal actual fun Throwable.toTransportFailure(): NetworkError? =
    toCommonTransportFailure() ?: when {
        this is DarwinHttpRequestException && origin.code == NSURLErrorTimedOut -> NetworkError.Timeout
        this is DarwinHttpRequestException && origin.code in offlineDarwinErrorCodes -> NetworkError.Offline
        this is IOException -> NetworkError.Offline
        else -> null
    }

private val offlineDarwinErrorCodes =
    setOf(
        NSURLErrorNotConnectedToInternet,
        NSURLErrorCannotConnectToHost,
        NSURLErrorCannotFindHost,
        NSURLErrorNetworkConnectionLost,
        NSURLErrorDNSLookupFailed,
        NSURLErrorInternationalRoamingOff,
        NSURLErrorDataNotAllowed,
    )
