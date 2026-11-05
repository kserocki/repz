package com.repz.app.feature.auth.impl.presentation.common

import com.repz.app.core.domain.result.ValidationError

/**
 * `Password.forLogIn`/`Password.forRegistration` only ever fail with these two [ValidationError]
 * cases; the `else` only guards against a domain change this screen hasn't been updated for.
 */
internal fun ValidationError.toPasswordFieldError(): PasswordFieldError =
    when (this) {
        ValidationError.PasswordMissing -> PasswordFieldError.Missing
        is ValidationError.PasswordTooShort -> PasswordFieldError.TooShort(minLength)
        is ValidationError.PasswordTooLong -> PasswordFieldError.TooLong(maxLength)
        else -> error("Password validation produced an unexpected error: $this")
    }

/** `DisplayName.of` only ever fails with these two [ValidationError] cases. */
internal fun ValidationError.toDisplayNameFieldError(): DisplayNameFieldError =
    when (this) {
        ValidationError.NameBlank -> DisplayNameFieldError.Blank
        is ValidationError.NameTooLong -> DisplayNameFieldError.TooLong(maxLength)
        else -> error("Display name validation produced an unexpected error: $this")
    }
