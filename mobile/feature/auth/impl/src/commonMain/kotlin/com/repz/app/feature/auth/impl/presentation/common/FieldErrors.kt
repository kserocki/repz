package com.repz.app.feature.auth.impl.presentation.common

/** UI-level error for the email field, turned into a string by the screen (AGENTS.md §4). */
sealed interface EmailFieldError {
    data object Invalid : EmailFieldError

    data object Taken : EmailFieldError
}

/** UI-level error for a password field, turned into a string by the screen. */
sealed interface PasswordFieldError {
    data object Missing : PasswordFieldError

    data class TooShort(
        val minLength: Int,
    ) : PasswordFieldError

    data class TooLong(
        val maxLength: Int,
    ) : PasswordFieldError
}

/** UI-level error for the display name field, turned into a string by the screen. */
sealed interface DisplayNameFieldError {
    data object Blank : DisplayNameFieldError

    data class TooLong(
        val maxLength: Int,
    ) : DisplayNameFieldError
}
