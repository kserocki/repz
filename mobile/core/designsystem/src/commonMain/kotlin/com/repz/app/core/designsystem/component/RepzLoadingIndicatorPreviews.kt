package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

// Preview-only specimen text (not user-facing): AGENTS.md §4 Strings covers product copy, not a
// design-system preview's own label.
private const val PREVIEW_LOADING_DESCRIPTION = "Loading"

@Composable
private fun RepzLoadingIndicatorShowcase() {
    Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        RepzLoadingIndicator(contentDescription = PREVIEW_LOADING_DESCRIPTION)
        RepzLoadingIndicator(contentDescription = PREVIEW_LOADING_DESCRIPTION, contained = true)
    }
}

@Preview
@Composable
private fun RepzLoadingIndicatorLightPreview() {
    RepzTheme(darkTheme = false) { RepzLoadingIndicatorShowcase() }
}

@Preview
@Composable
private fun RepzLoadingIndicatorDarkPreview() {
    RepzTheme(darkTheme = true) { RepzLoadingIndicatorShowcase() }
}
