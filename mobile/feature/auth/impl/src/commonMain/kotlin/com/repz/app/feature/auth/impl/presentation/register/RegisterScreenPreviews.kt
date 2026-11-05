package com.repz.app.feature.auth.impl.presentation.register

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.component.rememberRepzSnackbarHostState
import com.repz.app.core.designsystem.theme.RepzTheme
import com.repz.app.core.domain.model.account.Password
import com.repz.app.feature.auth.impl.presentation.common.DisplayNameFieldError
import com.repz.app.feature.auth.impl.presentation.common.EmailFieldError
import com.repz.app.feature.auth.impl.presentation.common.PasswordFieldError

/** Shared by this file and `RegisterScreenPreviews2.kt` (both in this package, `internal` so no
 * import is needed); `RegisterScreen.kt` keeps its own single preview for Konsist N4. */
@Composable
internal fun RegisterPreview(
    darkTheme: Boolean,
    state: RegisterUiState,
) {
    val focusRequesters = remember { RegisterField.entries.associateWith { FocusRequester() } }
    val snackbarHostState = rememberRepzSnackbarHostState()
    RepzTheme(darkTheme = darkTheme) {
        RegisterContent(
            state = state,
            onIntent = {},
            onBackClick = {},
            focusRequesters = focusRequesters,
            snackbarHostState = snackbarHostState,
        )
    }
}

internal val RegisterEmptyState = RegisterUiState()
internal val RegisterFieldErrorsState =
    RegisterUiState(
        displayName = "",
        email = "not-an-email",
        password = "short",
        displayNameError = DisplayNameFieldError.Blank,
        emailError = EmailFieldError.Invalid,
        passwordError = PasswordFieldError.TooShort(Password.NEW_PASSWORD_LENGTH.first),
    )
internal val RegisterEmailTakenState =
    RegisterUiState(
        displayName = "Lena",
        email = "lena@example.com",
        password = "correct-horse-battery",
        emailError = EmailFieldError.Taken,
    )

@Preview
@Composable
private fun RegisterEmptyLightPreview() = RegisterPreview(darkTheme = false, state = RegisterEmptyState)

@Preview
@Composable
private fun RegisterEmptyDarkPreview() = RegisterPreview(darkTheme = true, state = RegisterEmptyState)

@Preview
@Composable
private fun RegisterFieldErrorsLightPreview() = RegisterPreview(darkTheme = false, state = RegisterFieldErrorsState)

@Preview
@Composable
private fun RegisterFieldErrorsDarkPreview() = RegisterPreview(darkTheme = true, state = RegisterFieldErrorsState)

@Preview
@Composable
private fun RegisterEmailTakenLightPreview() = RegisterPreview(darkTheme = false, state = RegisterEmailTakenState)

@Preview
@Composable
private fun RegisterEmailTakenDarkPreview() = RegisterPreview(darkTheme = true, state = RegisterEmailTakenState)

@Preview(locale = "pl")
@Composable
private fun RegisterFieldErrorsPolishPreview() = RegisterPreview(darkTheme = false, state = RegisterFieldErrorsState)
