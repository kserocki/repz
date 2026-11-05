package com.repz.app.feature.auth.impl.presentation.welcome

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzTheme

@Composable
private fun WelcomePreview(
    darkTheme: Boolean,
    initialPage: Int,
) {
    val state = remember { WelcomeUiState() }
    val pagerState = rememberPagerState(initialPage = initialPage) { state.pages.size }
    RepzTheme(darkTheme = darkTheme) {
        WelcomeContent(state = state, pagerState = pagerState, onIntent = {})
    }
}

@Preview
@Composable
private fun WelcomePage1LightPreview() = WelcomePreview(darkTheme = false, initialPage = 0)

@Preview
@Composable
private fun WelcomePage1DarkPreview() = WelcomePreview(darkTheme = true, initialPage = 0)

@Preview
@Composable
private fun WelcomePage2LightPreview() = WelcomePreview(darkTheme = false, initialPage = 1)

@Preview
@Composable
private fun WelcomePage2DarkPreview() = WelcomePreview(darkTheme = true, initialPage = 1)

@Preview
@Composable
private fun WelcomePage3LightPreview() = WelcomePreview(darkTheme = false, initialPage = 2)

@Preview
@Composable
private fun WelcomePage3DarkPreview() = WelcomePreview(darkTheme = true, initialPage = 2)

@Preview(locale = "pl")
@Composable
private fun WelcomePage1PolishPreview() = WelcomePreview(darkTheme = false, initialPage = 0)

@Preview(locale = "pl")
@Composable
private fun WelcomePage2PolishPreview() = WelcomePreview(darkTheme = false, initialPage = 1)

@Preview(locale = "pl")
@Composable
private fun WelcomePage3PolishPreview() = WelcomePreview(darkTheme = false, initialPage = 2)
