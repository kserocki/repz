package com.repz.app.core.network.result

/** Every way a `core:network` call can fail, typed so no exception crosses this module's boundary. */
sealed interface NetworkError {
    /** No route to the server: DNS failure, connection refused or reset, no network interface up. */
    data object Offline : NetworkError

    /** The request or the connection timed out (ADR-0011's grace window keeps this recoverable). */
    data object Timeout : NetworkError

    /** The refresh token was rejected (expired, reused, or logged out elsewhere); tokens are already cleared. */
    data object SessionExpired : NetworkError

    /** Any non-2xx response. [problem] is the parsed `application/problem+json` body, or `null` when the body wasn't one. */
    data class Api(
        val status: Int,
        val problem: ApiProblem?,
    ) : NetworkError

    /** A 2xx (or problem+json) body that doesn't decode as the expected shape. */
    data class MalformedResponse(
        val message: String,
    ) : NetworkError
}
