package com.repz.app.core.domain.model.account

import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import kotlin.jvm.JvmInline

/**
 * Never trimmed (Product decisions 2026-11-01): a password made of spaces is still a password, and
 * trimming it would silently change what the user typed. [toString] masks the value so it never
 * lands in a log by accident.
 */
@JvmInline
value class Password private constructor(
    val value: String,
) {
    override fun toString(): String = "Password(***)"

    companion object {
        val NEW_PASSWORD_LENGTH = 8..128
        const val MAX_LENGTH = 128

        /** Registration: 8-128 characters, matching the server's minimum. */
        fun forRegistration(raw: String): DomainResult<Password, ValidationError> =
            when {
                raw.length < NEW_PASSWORD_LENGTH.first -> DomainResult.Failure(ValidationError.PasswordTooShort(NEW_PASSWORD_LENGTH.first))
                raw.length > NEW_PASSWORD_LENGTH.last -> DomainResult.Failure(ValidationError.PasswordTooLong(NEW_PASSWORD_LENGTH.last))
                else -> DomainResult.Success(Password(raw))
            }

        /** Login: only non-empty is checked client-side; the server alone knows if it's correct. */
        fun forLogIn(raw: String): DomainResult<Password, ValidationError> =
            when {
                raw.isEmpty() -> DomainResult.Failure(ValidationError.PasswordMissing)
                raw.length > MAX_LENGTH -> DomainResult.Failure(ValidationError.PasswordTooLong(MAX_LENGTH))
                else -> DomainResult.Success(Password(raw))
            }
    }
}
