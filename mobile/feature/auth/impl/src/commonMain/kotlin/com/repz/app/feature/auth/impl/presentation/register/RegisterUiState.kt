package com.repz.app.feature.auth.impl.presentation.register

import com.repz.app.feature.auth.impl.presentation.common.DisplayNameFieldError
import com.repz.app.feature.auth.impl.presentation.common.EmailFieldError
import com.repz.app.feature.auth.impl.presentation.common.PasswordFieldError

/** Register screen state (AGENTS.md §4, MVI-lite). */
data class RegisterUiState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val displayNameError: DisplayNameFieldError? = null,
    val emailError: EmailFieldError? = null,
    val passwordError: PasswordFieldError? = null,
    val isPasswordVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    val isOfflineBannerVisible: Boolean = false,
)
