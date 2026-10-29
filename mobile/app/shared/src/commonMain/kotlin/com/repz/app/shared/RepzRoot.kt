package com.repz.app.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.RepzBrandColors

/**
 * The app's root composable, hosted by both platform shells right after Koin has started.
 *
 * Fills the window with the plain brand background (paper by day, ink by night) that the static
 * OS splash already showed, so the hand-off is a flat colour match with no flash and no symbol.
 *
 * TODO(REP-28): replace the placeholder with the NavDisplay app shell.
 */
@Composable
fun RepzRoot(modifier: Modifier = Modifier) {
    LaunchPlaceholder(darkTheme = isSystemInDarkTheme(), modifier = modifier)
}

@Composable
private fun LaunchPlaceholder(darkTheme: Boolean, modifier: Modifier = Modifier) {
    val backgroundColor = if (darkTheme) RepzBrandColors.Ink else RepzBrandColors.Paper
    Box(modifier = modifier.fillMaxSize().background(backgroundColor))
}

@Preview
@Composable
private fun LaunchPlaceholderLightPreview() {
    LaunchPlaceholder(darkTheme = false)
}

@Preview
@Composable
private fun LaunchPlaceholderDarkPreview() {
    LaunchPlaceholder(darkTheme = true)
}
