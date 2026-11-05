package com.repz.app.feature.auth.impl.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repz.app.core.domain.auth.AccountField
import com.repz.app.core.domain.auth.AuthError
import com.repz.app.core.domain.model.account.DisplayName
import com.repz.app.core.domain.model.account.EmailAddress
import com.repz.app.core.domain.model.account.Password
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.feature.auth.impl.domain.RegisterUseCase
import com.repz.app.feature.auth.impl.presentation.common.DisplayNameFieldError
import com.repz.app.feature.auth.impl.presentation.common.EmailFieldError
import com.repz.app.feature.auth.impl.presentation.common.PasswordFieldError
import com.repz.app.feature.auth.impl.presentation.common.toDisplayNameFieldError
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
 * On a successful registration [RegisterUiState.isSubmitting] stays `true`: navigation to the
 * signed-in shell is driven by the session, not by this ViewModel (ADR-0025).
 */
class RegisterViewModel(
    private val register: RegisterUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = mutableState.asStateFlow()

    private val mutableEffects = Channel<RegisterEffect>(Channel.BUFFERED)
    val effects: Flow<RegisterEffect> = mutableEffects.receiveAsFlow()

    fun onIntent(intent: RegisterIntent) {
        when (intent) {
            is RegisterIntent.DisplayNameChanged -> onFieldChanged(RegisterField.DisplayName, intent.value)
            is RegisterIntent.EmailChanged -> onFieldChanged(RegisterField.Email, intent.value)
            is RegisterIntent.PasswordChanged -> onFieldChanged(RegisterField.Password, intent.value)
            RegisterIntent.DisplayNameFocusLost -> onFieldFocusLost(RegisterField.DisplayName)
            RegisterIntent.EmailFocusLost -> onFieldFocusLost(RegisterField.Email)
            RegisterIntent.PasswordFocusLost -> onFieldFocusLost(RegisterField.Password)
            RegisterIntent.PasswordVisibilityToggled -> onPasswordVisibilityToggled()
            RegisterIntent.SubmitClicked -> onSubmitClicked()
        }
    }

    /** Typing never introduces a new error (Q11); it only re-validates a field already showing one. */
    private fun onFieldChanged(
        field: RegisterField,
        value: String,
    ) {
        mutableState.update { current ->
            when (field) {
                RegisterField.DisplayName -> {
                    current.copy(displayName = value, displayNameError = current.displayNameError?.let { validateDisplayName(value) })
                }

                RegisterField.Email -> {
                    current.copy(email = value, emailError = current.emailError?.let { validateEmail(value) })
                }

                RegisterField.Password -> {
                    current.copy(password = value, passwordError = current.passwordError?.let { validatePassword(value) })
                }
            }
        }
    }

    /** An untouched empty field stays quiet; any other field is (re-)validated. */
    private fun onFieldFocusLost(field: RegisterField) {
        mutableState.update { current ->
            when (field) {
                RegisterField.DisplayName -> {
                    if (current.displayName.isEmpty() && current.displayNameError == null) {
                        current
                    } else {
                        current.copy(displayNameError = validateDisplayName(current.displayName))
                    }
                }

                RegisterField.Email -> {
                    when {
                        // Taken only clears on an edit (handled in onFieldChanged); losing focus
                        // without changing the value must not re-validate it away.
                        current.emailError == EmailFieldError.Taken -> current

                        current.email.isEmpty() && current.emailError == null -> current

                        else -> current.copy(emailError = validateEmail(current.email))
                    }
                }

                RegisterField.Password -> {
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

        val displayNameResult = DisplayName.of(mutableState.value.displayName)
        val emailResult = EmailAddress.of(mutableState.value.email)
        val passwordResult = Password.forRegistration(mutableState.value.password)
        val displayNameError = (displayNameResult as? DomainResult.Failure)?.error?.toDisplayNameFieldError()
        val emailError = if (emailResult is DomainResult.Failure) EmailFieldError.Invalid else null
        val passwordError = (passwordResult as? DomainResult.Failure)?.error?.toPasswordFieldError()

        if (displayNameError != null || emailError != null || passwordError != null) {
            mutableState.update {
                it.copy(displayNameError = displayNameError, emailError = emailError, passwordError = passwordError)
            }
            val firstInvalidField =
                when {
                    displayNameError != null -> RegisterField.DisplayName
                    emailError != null -> RegisterField.Email
                    else -> RegisterField.Password
                }
            sendEffect(RegisterEffect.FocusField(firstInvalidField))
            return
        }

        val displayName = (displayNameResult as DomainResult.Success).value
        val email = (emailResult as DomainResult.Success).value
        val password = (passwordResult as DomainResult.Success).value
        mutableState.update { it.copy(isSubmitting = true, isOfflineBannerVisible = false) }

        viewModelScope.launch {
            when (val result = register(email, password, displayName)) {
                is DomainResult.Success -> Unit
                is DomainResult.Failure -> handleFailure(result.error)
            }
        }
    }

    private fun handleFailure(error: AuthError) {
        when (error) {
            AuthError.EmailTaken -> {
                mutableState.update { it.copy(isSubmitting = false, emailError = EmailFieldError.Taken) }
                sendEffect(RegisterEffect.FocusField(RegisterField.Email))
            }

            AuthError.NoConnection -> {
                mutableState.update { it.copy(isSubmitting = false, isOfflineBannerVisible = true) }
            }

            is AuthError.FieldsRejected -> {
                applyFieldsRejected(error.fields)
            }

            // Registration never returns InvalidCredentials; an exhaustive match still has to cover it.
            AuthError.InvalidCredentials, AuthError.Unexpected -> {
                mutableState.update { it.copy(isSubmitting = false) }
                sendEffect(RegisterEffect.ShowUnexpectedError)
            }
        }
    }

    /**
     * The local validator should have caught these already, so re-running it against the current
     * value almost always still accepts it; the fixed fallback then describes the server's actual
     * rule. It only disagrees when the field changed while the request was in flight.
     */
    private fun applyFieldsRejected(fields: Set<AccountField>) {
        mutableState.update { current ->
            current.copy(
                isSubmitting = false,
                displayNameError =
                    if (AccountField.DisplayName in fields) {
                        validateDisplayName(current.displayName) ?: DisplayNameFieldError.Blank
                    } else {
                        null
                    },
                emailError = if (AccountField.Email in fields) validateEmail(current.email) ?: EmailFieldError.Invalid else null,
                passwordError =
                    if (AccountField.Password in fields) {
                        validatePassword(current.password) ?: PasswordFieldError.TooShort(Password.NEW_PASSWORD_LENGTH.first)
                    } else {
                        null
                    },
            )
        }

        val firstRejectedField =
            when {
                AccountField.DisplayName in fields -> RegisterField.DisplayName
                AccountField.Email in fields -> RegisterField.Email
                AccountField.Password in fields -> RegisterField.Password
                else -> null
            }
        firstRejectedField?.let { sendEffect(RegisterEffect.FocusField(it)) }
    }

    private fun sendEffect(effect: RegisterEffect) {
        viewModelScope.launch { mutableEffects.send(effect) }
    }
}

private fun validateDisplayName(raw: String): DisplayNameFieldError? =
    when (val result = DisplayName.of(raw)) {
        is DomainResult.Success -> null
        is DomainResult.Failure -> result.error.toDisplayNameFieldError()
    }

private fun validateEmail(raw: String): EmailFieldError? =
    when (EmailAddress.of(raw)) {
        is DomainResult.Success -> null
        is DomainResult.Failure -> EmailFieldError.Invalid
    }

private fun validatePassword(raw: String): PasswordFieldError? =
    when (val result = Password.forRegistration(raw)) {
        is DomainResult.Success -> null
        is DomainResult.Failure -> result.error.toPasswordFieldError()
    }
