package com.repz.app.feature.auth.impl.presentation.register

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Stateful (ADR-0005): resolves [RegisterViewModel] itself, forwards [RegisterEffect]s (focus,
 * the unexpected-error snackbar) and the back button to [navigator]. Navigation to the signed-in
 * shell on success is not handled here: it follows the session (ADR-0025). The rest of the
 * per-state previews (ADR-0005) live in `RegisterScreenPreviews.kt`/`RegisterScreenPreviews2.kt`. */
@Composable
fun RegisterScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<RegisterViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequesters = remember { RegisterField.entries.associateWith { FocusRequester() } }
    val snackbarHostState = rememberRepzSnackbarHostState()
    val unexpectedErrorMessage = stringResource(Res.string.error_unexpected)
    val retryLabel = stringResource(Res.string.action_retry)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is RegisterEffect.FocusField -> {
                    focusRequesters.getValue(effect.field).requestFocus()
                }

                RegisterEffect.ShowUnexpectedError -> {
                    val tapped = snackbarHostState.showWithAction(unexpectedErrorMessage, retryLabel)
                    if (tapped) viewModel.onIntent(RegisterIntent.SubmitClicked)
                }
            }
        }
    }

    RegisterContent(
        state = state,
        onIntent = viewModel::onIntent,
        onBackClick = { navigator.goBack() },
        focusRequesters = focusRequesters,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun RegisterScreenDefaultPreview() {
    val focusRequesters = remember { RegisterField.entries.associateWith { FocusRequester() } }
    val snackbarHostState = rememberRepzSnackbarHostState()
    RepzTheme(darkTheme = false) {
        RegisterContent(
            state = RegisterUiState(),
            onIntent = {},
            onBackClick = {},
            focusRequesters = focusRequesters,
            snackbarHostState = snackbarHostState,
        )
    }
}
