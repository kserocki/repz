package com.repz.app.core.designsystem.logo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

private val DETERMINATE_STEPS = listOf(0f, 1f / 3f, 2f / 3f, 1f)

// Preview-only specimen text (not user-facing): AGENTS.md §4 Strings covers product copy, not a
// design-system preview's own label.
private const val PREVIEW_LOADER_DESCRIPTION = "Loading"

@Composable
private fun RepzLoaderShowcase() {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
            for (size in RepzLoaderSize.entries) {
                RepzLoader(contentDescription = PREVIEW_LOADER_DESCRIPTION, size = size)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
            for (progress in DETERMINATE_STEPS) {
                RepzLoader(contentDescription = PREVIEW_LOADER_DESCRIPTION, progress = progress)
            }
        }
    }
}

@Preview
@Composable
private fun RepzLoaderLightPreview() {
    RepzTheme(darkTheme = false) { RepzLoaderShowcase() }
}

@Preview
@Composable
private fun RepzLoaderDarkPreview() {
    RepzTheme(darkTheme = true) { RepzLoaderShowcase() }
}
