package com.repz.app.core.testing.auth

import com.repz.app.core.domain.auth.AuthError
import com.repz.app.core.domain.auth.AuthRepository
import com.repz.app.core.domain.model.User
import com.repz.app.core.domain.model.account.DisplayName
import com.repz.app.core.domain.model.account.EmailAddress
import com.repz.app.core.domain.model.account.Password
import com.repz.app.core.domain.result.DomainResult
import kotlinx.coroutines.CompletableDeferred

/**
 * A hand-written fake (N7). [logInResult]/[registerResult] are returned as-is; set an
 * [logInGate]/[registerGate] to suspend the call until the test completes it, e.g. to assert a
 * ViewModel's `isSubmitting` state mid-flight.
 */
class FakeAuthRepository(
    var logInResult: DomainResult<User, AuthError> = DomainResult.Failure(AuthError.Unexpected),
    var registerResult: DomainResult<User, AuthError> = DomainResult.Failure(AuthError.Unexpected),
) : AuthRepository {
    var logInGate: CompletableDeferred<Unit>? = null
    var registerGate: CompletableDeferred<Unit>? = null

    private val mutableLogInCalls = mutableListOf<Pair<EmailAddress, Password>>()
    val logInCalls: List<Pair<EmailAddress, Password>> get() = mutableLogInCalls

    private val mutableRegisterCalls = mutableListOf<Triple<EmailAddress, Password, DisplayName>>()
    val registerCalls: List<Triple<EmailAddress, Password, DisplayName>> get() = mutableRegisterCalls

    override suspend fun register(
        email: EmailAddress,
        password: Password,
        displayName: DisplayName,
    ): DomainResult<User, AuthError> {
        mutableRegisterCalls += Triple(email, password, displayName)
        registerGate?.await()
        return registerResult
    }

    override suspend fun logIn(
        email: EmailAddress,
        password: Password,
    ): DomainResult<User, AuthError> {
        mutableLogInCalls += email to password
        logInGate?.await()
        return logInResult
    }
}
