package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

@Composable
private fun RepzTopAppBarShowcase() {
    val sampleTitle = "Create your account"
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        RepzTopAppBar()
        RepzTopAppBar(title = sampleTitle)
        RepzTopAppBar(title = sampleTitle, onBackClick = {})
    }
}

/** Expanded (non-collapsing, `scrollState = null`), with and without a back action, for both the
 * medium and the large bar: T5's one-off "fits over a short scrollable list" collapsing behaviour
 * needs a real scroll gesture, shown instead in `components-bars` (R12). */
@Composable
private fun RepzMediumLargeTopAppBarShowcase() {
    val sampleTitle = "Templates"
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        RepzMediumTopAppBar(title = sampleTitle)
        RepzMediumTopAppBar(title = sampleTitle, onBackClick = {})
        RepzLargeTopAppBar(title = sampleTitle)
        RepzLargeTopAppBar(title = sampleTitle, onBackClick = {})
    }
}

@Preview
@Composable
private fun RepzTopAppBarLightPreview() {
    RepzTheme(darkTheme = false) { RepzTopAppBarShowcase() }
}

@Preview
@Composable
private fun RepzTopAppBarDarkPreview() {
    RepzTheme(darkTheme = true) { RepzTopAppBarShowcase() }
}

@Preview
@Composable
private fun RepzMediumLargeTopAppBarLightPreview() {
    RepzTheme(darkTheme = false) { RepzMediumLargeTopAppBarShowcase() }
}

@Preview
@Composable
private fun RepzMediumLargeTopAppBarDarkPreview() {
    RepzTheme(darkTheme = true) { RepzMediumLargeTopAppBarShowcase() }
}
