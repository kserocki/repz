package com.repz.app.feature.auth.impl.presentation.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.component.rememberRepzSnackbarHostState
import com.repz.app.core.designsystem.theme.RepzTheme

/** Shared by this file and `LoginScreenPreviews2.kt` (both in this package, `internal` so no
 * import is needed); `LoginScreen.kt` keeps its own single preview for Konsist N4. */
@Composable
internal fun LoginPreview(
    darkTheme: Boolean,
    state: LoginUiState,
) {
    val focusRequesters = remember { LoginField.entries.associateWith { FocusRequester() } }
    val snackbarHostState = rememberRepzSnackbarHostState()
    RepzTheme(darkTheme = darkTheme) {
        LoginContent(
            state = state,
            onIntent = {},
            onBackClick = {},
            focusRequesters = focusRequesters,
            snackbarHostState = snackbarHostState,
        )
    }
}

internal val LoginEmptyState = LoginUiState()
internal val LoginFilledState = LoginUiState(email = "lena@example.com", password = "correct-horse-battery")
internal val LoginCredentialsRejectedState =
    LoginUiState(email = "lena@example.com", password = "wrong-password", areCredentialsRejected = true)

@Preview
@Composable
private fun LoginEmptyLightPreview() = LoginPreview(darkTheme = false, state = LoginEmptyState)

@Preview
@Composable
private fun LoginEmptyDarkPreview() = LoginPreview(darkTheme = true, state = LoginEmptyState)

@Preview
@Composable
private fun LoginFilledLightPreview() = LoginPreview(darkTheme = false, state = LoginFilledState)

@Preview
@Composable
private fun LoginFilledDarkPreview() = LoginPreview(darkTheme = true, state = LoginFilledState)

@Preview
@Composable
private fun LoginCredentialsRejectedLightPreview() = LoginPreview(darkTheme = false, state = LoginCredentialsRejectedState)

@Preview
@Composable
private fun LoginCredentialsRejectedDarkPreview() = LoginPreview(darkTheme = true, state = LoginCredentialsRejectedState)

@Preview(locale = "pl")
@Composable
private fun LoginCredentialsRejectedPolishPreview() = LoginPreview(darkTheme = false, state = LoginCredentialsRejectedState)
