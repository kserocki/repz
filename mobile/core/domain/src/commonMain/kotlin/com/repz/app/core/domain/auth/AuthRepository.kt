package com.repz.app.core.domain.auth

import com.repz.app.core.domain.model.User
import com.repz.app.core.domain.model.account.DisplayName
import com.repz.app.core.domain.model.account.EmailAddress
import com.repz.app.core.domain.model.account.Password
import com.repz.app.core.domain.result.DomainResult

/**
 * Registration and login against the API. On success the session is started (persisted where
 * possible) and the session state becomes signed in.
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

    /** Ends the session on this device at once, offline too; the server-side revoke is best effort. */
    suspend fun logOut()
}
