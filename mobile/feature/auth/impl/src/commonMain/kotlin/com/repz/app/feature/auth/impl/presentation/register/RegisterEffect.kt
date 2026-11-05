package com.repz.app.feature.auth.impl.presentation.register

sealed interface RegisterEffect {
    data class FocusField(
        val field: RegisterField,
    ) : RegisterEffect

    /** Snackbar "Something went wrong. Try again" with a Retry action that re-sends [RegisterIntent.SubmitClicked]. */
    data object ShowUnexpectedError : RegisterEffect
}

enum class RegisterField { DisplayName, Email, Password }
