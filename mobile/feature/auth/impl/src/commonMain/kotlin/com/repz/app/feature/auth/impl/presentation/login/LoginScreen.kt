package com.repz.app.feature.auth.impl.presentation.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.repz.app.core.designsystem.component.rememberRepzSnackbarHostState
import com.repz.app.core.designsystem.theme.RepzTheme
import com.repz.app.core.navigation.Navigator
import com.repz.app.feature.auth.impl.generated.resources.Res
import com.repz.app.feature.auth.impl.generated.resources.action_retry
import com.repz.app.feature.auth.impl.generated.resources.error_unexpected
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Stateful (ADR-0005): resolves [LoginViewModel] itself, forwards [LoginEffect]s (focus, the
 * unexpected-error snackbar) and the back button to [navigator]. Navigation to the signed-in
 * shell on success is not handled here: it follows the session (ADR-0025). The rest of the
 * per-state previews live in `LoginScreenPreviews.kt`. */
@Composable
fun LoginScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<LoginViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequesters = remember { LoginField.entries.associateWith { FocusRequester() } }
    val snackbarHostState = rememberRepzSnackbarHostState()
    val unexpectedErrorMessage = stringResource(Res.string.error_unexpected)
    val retryLabel = stringResource(Res.string.action_retry)
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LoginEffect.FocusField -> {
                    focusRequesters.getValue(effect.field).requestFocus()
                }

                LoginEffect.ShowUnexpectedError -> {
                    // Launched on its own, not awaited here: `showWithAction` suspends until the
                    // snackbar is dismissed, and awaiting it in this loop would block every later
                    // effect (e.g. a focus move on resubmit) until then.
                    scope.launch {
                        val tapped = snackbarHostState.showWithAction(unexpectedErrorMessage, retryLabel)
                        if (tapped) viewModel.onIntent(LoginIntent.SubmitClicked)
                    }
                }
            }
        }
    }

    LoginContent(
        state = state,
        onIntent = { intent ->
            // A fresh submit should never leave a stale error on screen covering the new attempt.
            if (intent is LoginIntent.SubmitClicked) snackbarHostState.dismissCurrent()
            viewModel.onIntent(intent)
        },
        onBackClick = { navigator.goBack() },
        focusRequesters = focusRequesters,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun LoginScreenDefaultPreview() {
    val focusRequesters = remember { LoginField.entries.associateWith { FocusRequester() } }
    val snackbarHostState = rememberRepzSnackbarHostState()
    RepzTheme(darkTheme = false) {
        LoginContent(
            state = LoginUiState(),
            onIntent = {},
            onBackClick = {},
            focusRequesters = focusRequesters,
            snackbarHostState = snackbarHostState,
        )
    }
}
