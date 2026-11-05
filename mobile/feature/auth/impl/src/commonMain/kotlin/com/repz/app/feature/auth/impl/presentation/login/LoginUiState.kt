package com.repz.app.feature.auth.impl.presentation.login

import com.repz.app.feature.auth.impl.presentation.common.EmailFieldError
import com.repz.app.feature.auth.impl.presentation.common.PasswordFieldError

/**
 * Log-in screen state (AGENTS.md §4, MVI-lite). [areCredentialsRejected] draws both fields in
 * error with one message under the password, instead of a per-field [emailError]/[passwordError].
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: EmailFieldError? = null,
    val passwordError: PasswordFieldError? = null,
    val areCredentialsRejected: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    val isOfflineBannerVisible: Boolean = false,
)
