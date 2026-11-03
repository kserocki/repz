package com.repz.app.shared.shell.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.RepzBrandColors

/**
 * The plain brand background (Paper by day, Ink by night) that the static OS splash already
 * showed, filling the window while the session restores so the hand-off is a flat colour match
 * with no flash and no symbol. The one-tone shift to the themed screen that follows is an
 * accepted trade-off.
 */
@Composable
internal fun BrandBackground(modifier: Modifier = Modifier) {
    BrandBackground(darkTheme = isSystemInDarkTheme(), modifier = modifier)
}

@Composable
private fun BrandBackground(
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (darkTheme) RepzBrandColors.Ink else RepzBrandColors.Paper
    Box(modifier = modifier.fillMaxSize().background(backgroundColor))
}

@Preview
@Composable
private fun BrandBackgroundLightPreview() {
    BrandBackground(darkTheme = false)
}

@Preview
@Composable
private fun BrandBackgroundDarkPreview() {
    BrandBackground(darkTheme = true)
}
