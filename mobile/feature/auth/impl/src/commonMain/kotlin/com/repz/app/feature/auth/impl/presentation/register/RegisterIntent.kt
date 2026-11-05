package com.repz.app.feature.auth.impl.presentation.register

sealed interface RegisterIntent {
    data class DisplayNameChanged(
        val value: String,
    ) : RegisterIntent

    data class EmailChanged(
        val value: String,
    ) : RegisterIntent

    data class PasswordChanged(
        val value: String,
    ) : RegisterIntent

    data object DisplayNameFocusLost : RegisterIntent

    data object EmailFocusLost : RegisterIntent

    data object PasswordFocusLost : RegisterIntent

    data object PasswordVisibilityToggled : RegisterIntent

    /** Also sent by the snackbar's Retry action and the password field's IME Done action. */
    data object SubmitClicked : RegisterIntent
}
