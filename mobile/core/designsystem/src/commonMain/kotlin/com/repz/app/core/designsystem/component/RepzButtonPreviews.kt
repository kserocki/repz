package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

// Board 04's own button labels, reused here as catalog/preview sample copy only.
private fun labelFor(style: RepzButtonStyle) =
    when (style) {
        RepzButtonStyle.Filled -> "Add set"
        RepzButtonStyle.Outlined -> "Keep training"
        RepzButtonStyle.Text -> "See all"
    }

/** One row per style, each showing small/medium, disabled and loading, so every state in
 * DESIGN_SYSTEM §9's button table is visible at once. */
@Composable
private fun RepzButtonShowcase() {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        for (style in RepzButtonStyle.entries) {
            val label = labelFor(style)
            Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
                RepzButton(label, onClick = {}, style = style, size = RepzButtonSize.Small)
                RepzButton(label, onClick = {}, style = style, size = RepzButtonSize.Medium)
                RepzButton(label, onClick = {}, style = style, enabled = false)
                RepzButton(label, onClick = {}, style = style, isLoading = true)
            }
        }
    }
}

@Preview
@Composable
private fun RepzButtonLightPreview() {
    RepzTheme(darkTheme = false) { RepzButtonShowcase() }
}

@Preview
@Composable
private fun RepzButtonDarkPreview() {
    RepzTheme(darkTheme = true) { RepzButtonShowcase() }
}
