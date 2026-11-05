package com.repz.app.core.domain.user

import com.repz.app.core.domain.result.DomainError

sealed interface UserRefreshError : DomainError {
    data object NoConnection : UserRefreshError

    /** The gate already moved to Welcome. */
    data object SessionExpired : UserRefreshError

    data object Unexpected : UserRefreshError
}
