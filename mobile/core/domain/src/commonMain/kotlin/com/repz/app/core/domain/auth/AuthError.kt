package com.repz.app.core.domain.auth

import com.repz.app.core.domain.result.DomainError

sealed interface AuthError : DomainError {
    data object InvalidCredentials : AuthError

    data object EmailTaken : AuthError

    data object NoConnection : AuthError

    /** The server rejected these fields (400 validation-error); the client rules should have caught it. */
    data class FieldsRejected(
        val fields: Set<AccountField>,
    ) : AuthError

    /** Timeout, 5xx, an unexpected status or an undecodable body. */
    data object Unexpected : AuthError
}

enum class AccountField {
    Email,
    Password,
    DisplayName,
}
