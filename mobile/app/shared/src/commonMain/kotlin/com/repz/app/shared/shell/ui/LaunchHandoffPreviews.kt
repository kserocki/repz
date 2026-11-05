package com.repz.app.shared.shell.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzTheme
import com.repz.app.shared.shell.LaunchHandoffPhase

@Composable
private fun LaunchHandoffPreview(
    phase: LaunchHandoffPhase,
    darkTheme: Boolean,
) {
    RepzTheme(darkTheme = darkTheme) {
        LaunchHandoff(phase = phase, onLogoFinish = {}, onFadeFinish = {})
    }
}

@Preview
@Composable
private fun LaunchHandoffHoldingSplashFrameLightPreview() {
    LaunchHandoffPreview(LaunchHandoffPhase.HoldingSplashFrame, darkTheme = false)
}

@Preview
@Composable
private fun LaunchHandoffHoldingSplashFrameDarkPreview() {
    LaunchHandoffPreview(LaunchHandoffPhase.HoldingSplashFrame, darkTheme = true)
}

@Preview
@Composable
private fun LaunchHandoffLoadingTheBarLightPreview() {
    LaunchHandoffPreview(LaunchHandoffPhase.LoadingTheBar, darkTheme = false)
}

@Preview
@Composable
private fun LaunchHandoffLoadingTheBarDarkPreview() {
    LaunchHandoffPreview(LaunchHandoffPhase.LoadingTheBar, darkTheme = true)
}

@Preview
@Composable
private fun LaunchHandoffFadingOutLightPreview() {
    LaunchHandoffPreview(LaunchHandoffPhase.FadingOut, darkTheme = false)
}

@Preview
@Composable
private fun LaunchHandoffFadingOutDarkPreview() {
    LaunchHandoffPreview(LaunchHandoffPhase.FadingOut, darkTheme = true)
}
