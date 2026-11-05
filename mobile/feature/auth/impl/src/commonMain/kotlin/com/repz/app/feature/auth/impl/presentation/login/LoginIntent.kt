package com.repz.app.feature.auth.impl.presentation.login

sealed interface LoginIntent {
    data class EmailChanged(
        val value: String,
    ) : LoginIntent

    data class PasswordChanged(
        val value: String,
    ) : LoginIntent

    data object EmailFocusLost : LoginIntent

    data object PasswordFocusLost : LoginIntent

    data object PasswordVisibilityToggled : LoginIntent

    /** Also sent by the snackbar's Retry action and the password field's IME Done action. */
    data object SubmitClicked : LoginIntent
}
