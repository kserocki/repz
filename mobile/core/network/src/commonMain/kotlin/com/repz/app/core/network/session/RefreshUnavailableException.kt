package com.repz.app.core.network.session

import com.repz.app.core.network.result.NetworkError

/**
 * Thrown by [TokenRefresher] when a refresh attempt fails for a reason that doesn't invalidate the
 * refresh token (offline, timeout, a 5xx): tokens are kept, and [error] is surfaced to the original
 * caller by [apiCall][com.repz.app.core.network.result.apiCall] instead of a generic `Api(401, …)`.
 */
internal class RefreshUnavailableException(
    val error: NetworkError,
) : Exception("token refresh unavailable: $error")
