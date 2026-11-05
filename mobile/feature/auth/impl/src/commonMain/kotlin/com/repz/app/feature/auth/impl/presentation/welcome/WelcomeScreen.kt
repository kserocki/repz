package com.repz.app.feature.auth.impl.presentation.welcome

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzTheme
import com.repz.app.core.navigation.Navigator
import com.repz.app.feature.auth.api.LoginRoute
import com.repz.app.feature.auth.api.RegisterRoute

/** Stateful (ADR-0005): no ViewModel (see [WelcomeUiState]), so it only turns
 * [WelcomeIntent.CreateAccountClicked]/[WelcomeIntent.LogInClicked] into navigation. The rest of
 * the per-page previews live in `WelcomeScreenPreviews.kt`. */
@Composable
fun WelcomeScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val state = remember { WelcomeUiState() }
    val pagerState = rememberPagerState { state.pages.size }

    WelcomeContent(
        state = state,
        pagerState = pagerState,
        onIntent = { intent ->
            when (intent) {
                WelcomeIntent.CreateAccountClicked -> navigator.navigate(RegisterRoute)
                WelcomeIntent.LogInClicked -> navigator.navigate(LoginRoute)
            }
        },
        modifier = modifier,
    )
}

@Preview
@Composable
private fun WelcomeScreenDefaultPreview() {
    val state = remember { WelcomeUiState() }
    val pagerState = rememberPagerState { state.pages.size }
    RepzTheme(darkTheme = false) {
        WelcomeContent(state = state, pagerState = pagerState, onIntent = {})
    }
}
