package com.repz.app.core.domain.auth

import com.repz.app.core.domain.model.User
import com.repz.app.core.domain.model.account.DisplayName
import com.repz.app.core.domain.model.account.EmailAddress
import com.repz.app.core.domain.model.account.Password
import com.repz.app.core.domain.result.DomainResult

/**
 * Registration and login against the API. On success the session is started (persisted where
 * possible) and the session state becomes signed in. Logging out is not here: it wipes the local
 * database and is a separate, later concern.
 */
interface AuthRepository {
    suspend fun register(
        email: EmailAddress,
        password: Password,
        displayName: DisplayName,
    ): DomainResult<User, AuthError>

    suspend fun logIn(
        email: EmailAddress,
        password: Password,
    ): DomainResult<User, AuthError>
}
