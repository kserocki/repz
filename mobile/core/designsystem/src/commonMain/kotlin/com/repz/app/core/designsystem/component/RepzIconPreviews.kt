package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

@Composable
private fun RepzIconShowcase() {
    Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.md)) {
        for (icon in RepzIcon.entries) {
            RepzIcon(icon = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Preview
@Composable
private fun RepzIconLightPreview() {
    RepzTheme(darkTheme = false) { RepzIconShowcase() }
}

@Preview
@Composable
private fun RepzIconDarkPreview() {
    RepzTheme(darkTheme = true) { RepzIconShowcase() }
}
