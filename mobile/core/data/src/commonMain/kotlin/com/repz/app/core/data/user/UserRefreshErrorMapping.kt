package com.repz.app.core.data.user

import com.repz.app.core.domain.user.UserRefreshError
import com.repz.app.core.network.result.NetworkError

/** A stopped local API fails the connection the same as a true offline device (ADR-0011 amendment). */
internal fun NetworkError.toUserRefreshError(): UserRefreshError =
    when (this) {
        NetworkError.Offline -> UserRefreshError.NoConnection
        NetworkError.SessionExpired -> UserRefreshError.SessionExpired
        is NetworkError.Api, NetworkError.Timeout, is NetworkError.MalformedResponse -> UserRefreshError.Unexpected
    }
