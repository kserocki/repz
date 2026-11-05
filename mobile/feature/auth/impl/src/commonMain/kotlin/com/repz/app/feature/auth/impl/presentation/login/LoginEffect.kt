package com.repz.app.feature.auth.impl.presentation.login

sealed interface LoginEffect {
    data class FocusField(
        val field: LoginField,
    ) : LoginEffect

    /** Snackbar "Something went wrong. Try again" with a Retry action that re-sends [LoginIntent.SubmitClicked]. */
    data object ShowUnexpectedError : LoginEffect
}

enum class LoginField { Email, Password }
