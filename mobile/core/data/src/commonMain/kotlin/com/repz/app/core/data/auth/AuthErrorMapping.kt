package com.repz.app.core.data.auth

import com.repz.app.core.domain.auth.AccountField
import com.repz.app.core.domain.auth.AuthError
import com.repz.app.core.network.result.ApiProblem
import com.repz.app.core.network.result.NetworkError
import com.repz.app.core.network.result.ProblemType

private const val HTTP_BAD_REQUEST = 400
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_CONFLICT = 409

/**
 * ADR-0011 amendment: a stopped local API fails the connection, so [NetworkError.Offline] maps to
 * [AuthError.NoConnection] the same as a true offline device. `SessionExpired` is unreachable here:
 * register/login always go over the public client, which never refreshes.
 */
internal fun NetworkError.toAuthError(): AuthError =
    when (this) {
        is NetworkError.Api -> apiErrorToAuthError(this)
        NetworkError.Offline -> AuthError.NoConnection
        NetworkError.Timeout, NetworkError.SessionExpired -> AuthError.Unexpected
        is NetworkError.MalformedResponse -> AuthError.Unexpected
    }

private fun apiErrorToAuthError(error: NetworkError.Api): AuthError {
    val problem = error.problem
    val status = error.status
    return when {
        status == HTTP_UNAUTHORIZED && problem?.type == ProblemType.InvalidCredentials -> AuthError.InvalidCredentials
        status == HTTP_CONFLICT && problem?.type == ProblemType.EmailTaken -> AuthError.EmailTaken
        status == HTTP_BAD_REQUEST && problem?.type == ProblemType.ValidationError -> problem.toFieldsRejectedOrUnexpected()
        else -> AuthError.Unexpected
    }
}

/**
 * The client's own rules should have caught these; any path the server names that we don't
 * recognise falls back to [AuthError.Unexpected].
 */
private fun ApiProblem.toFieldsRejectedOrUnexpected(): AuthError {
    val fields = fieldErrors.mapNotNull { it.path.toAccountFieldOrNull() }.toSet()
    return if (fields.isEmpty()) AuthError.Unexpected else AuthError.FieldsRejected(fields)
}

private fun String.toAccountFieldOrNull(): AccountField? =
    when (this) {
        "email" -> AccountField.Email
        "password" -> AccountField.Password
        "displayName" -> AccountField.DisplayName
        else -> null
    }
