package com.repz.app.feature.auth.impl.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repz.app.core.domain.auth.AccountField
import com.repz.app.core.domain.auth.AuthError
import com.repz.app.core.domain.model.account.EmailAddress
import com.repz.app.core.domain.model.account.Password
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.feature.auth.impl.domain.LogInUseCase
import com.repz.app.feature.auth.impl.presentation.common.EmailFieldError
import com.repz.app.feature.auth.impl.presentation.common.PasswordFieldError
import com.repz.app.feature.auth.impl.presentation.common.toPasswordFieldError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * On a successful login [LoginUiState.isSubmitting] stays `true`: navigation to the signed-in
 * shell is driven by the session, not by this ViewModel (ADR-0025), so the button must not flash
 * back to its idle state before the app shell swaps the stack.
 */
class LoginViewModel(
    private val logIn: LogInUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = mutableState.asStateFlow()

    private val mutableEffects = Channel<LoginEffect>(Channel.BUFFERED)
    val effects: Flow<LoginEffect> = mutableEffects.receiveAsFlow()

    fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.EmailChanged -> onFieldChanged(LoginField.Email, intent.value)
            is LoginIntent.PasswordChanged -> onFieldChanged(LoginField.Password, intent.value)
            LoginIntent.EmailFocusLost -> onFieldFocusLost(LoginField.Email)
            LoginIntent.PasswordFocusLost -> onFieldFocusLost(LoginField.Password)
            LoginIntent.PasswordVisibilityToggled -> onPasswordVisibilityToggled()
            LoginIntent.SubmitClicked -> onSubmitClicked()
        }
    }

    /**
     * Typing never introduces a new error (Q11); it only re-validates a field already showing
     * one, and always clears [LoginUiState.areCredentialsRejected].
     */
    private fun onFieldChanged(
        field: LoginField,
        value: String,
    ) {
        mutableState.update { current ->
            val updated =
                when (field) {
                    LoginField.Email -> {
                        current.copy(email = value, emailError = current.emailError?.let { validateEmail(value) })
                    }

                    LoginField.Password -> {
                        current.copy(password = value, passwordError = current.passwordError?.let { validatePassword(value) })
                    }
                }
            updated.copy(areCredentialsRejected = false)
        }
    }

    /** An untouched empty field stays quiet; any other field is (re-)validated. */
    private fun onFieldFocusLost(field: LoginField) {
        mutableState.update { current ->
            when (field) {
                LoginField.Email -> {
                    if (current.email.isEmpty() && current.emailError == null) {
                        current
                    } else {
                        current.copy(emailError = validateEmail(current.email))
                    }
                }

                LoginField.Password -> {
                    if (current.password.isEmpty() && current.passwordError == null) {
                        current
                    } else {
                        current.copy(passwordError = validatePassword(current.password))
                    }
                }
            }
        }
    }

    private fun onPasswordVisibilityToggled() {
        mutableState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    private fun onSubmitClicked() {
        if (mutableState.value.isSubmitting) return

        val emailResult = EmailAddress.of(mutableState.value.email)
        val passwordResult = Password.forLogIn(mutableState.value.password)
        val emailError = if (emailResult is DomainResult.Failure) EmailFieldError.Invalid else null
        val passwordError = (passwordResult as? DomainResult.Failure)?.error?.toPasswordFieldError()

        if (emailError != null || passwordError != null) {
            mutableState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            sendEffect(LoginEffect.FocusField(if (emailError != null) LoginField.Email else LoginField.Password))
            return
        }

        val email = (emailResult as DomainResult.Success).value
        val password = (passwordResult as DomainResult.Success).value
        mutableState.update { it.copy(isSubmitting = true, isOfflineBannerVisible = false) }

        viewModelScope.launch {
            when (val result = logIn(email, password)) {
                is DomainResult.Success -> Unit
                is DomainResult.Failure -> handleFailure(result.error)
            }
        }
    }

    private fun handleFailure(error: AuthError) {
        when (error) {
            AuthError.InvalidCredentials -> {
                mutableState.update { it.copy(isSubmitting = false, areCredentialsRejected = true) }
                sendEffect(LoginEffect.FocusField(LoginField.Password))
            }

            AuthError.EmailTaken -> {
                mutableState.update { it.copy(isSubmitting = false, emailError = EmailFieldError.Taken) }
                sendEffect(LoginEffect.FocusField(LoginField.Email))
            }

            AuthError.NoConnection -> {
                mutableState.update { it.copy(isSubmitting = false, isOfflineBannerVisible = true) }
            }

            is AuthError.FieldsRejected -> {
                applyFieldsRejected(error.fields)
            }

            AuthError.Unexpected -> {
                mutableState.update { it.copy(isSubmitting = false) }
                sendEffect(LoginEffect.ShowUnexpectedError)
            }
        }
    }

    /**
     * The local validator should have caught these already, so re-running it against the current
     * value almost always still accepts it; the fixed fallback then describes the server's actual
     * rule. It only disagrees when the field changed while the request was in flight. Login has no
     * minimum password length, so the fallback is [PasswordFieldError.Missing], not a register-only
     * length rule.
     */
    private fun applyFieldsRejected(fields: Set<AccountField>) {
        mutableState.update { current ->
            current.copy(
                isSubmitting = false,
                emailError = if (AccountField.Email in fields) validateEmail(current.email) ?: EmailFieldError.Invalid else null,
                passwordError =
                    if (AccountField.Password in fields) {
                        validatePassword(current.password) ?: PasswordFieldError.Missing
                    } else {
                        null
                    },
            )
        }

        val firstRejectedField =
            when {
                AccountField.Email in fields -> LoginField.Email
                AccountField.Password in fields -> LoginField.Password
                else -> null
            }
        firstRejectedField?.let { sendEffect(LoginEffect.FocusField(it)) }
    }

    private fun sendEffect(effect: LoginEffect) {
        viewModelScope.launch { mutableEffects.send(effect) }
    }
}

private fun validateEmail(raw: String): EmailFieldError? =
    when (EmailAddress.of(raw)) {
        is DomainResult.Success -> null
        is DomainResult.Failure -> EmailFieldError.Invalid
    }

private fun validatePassword(raw: String): PasswordFieldError? =
    when (val result = Password.forLogIn(raw)) {
        is DomainResult.Success -> null
        is DomainResult.Failure -> result.error.toPasswordFieldError()
    }
