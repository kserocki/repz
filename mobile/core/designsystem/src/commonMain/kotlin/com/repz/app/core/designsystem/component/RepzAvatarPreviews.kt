package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

@Composable
private fun RepzAvatarShowcase() {
    Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.md)) {
        RepzAvatar(monogram = "A")
        RepzAvatar(monogram = null)
    }
}

@Preview
@Composable
private fun RepzAvatarLightPreview() {
    RepzTheme(darkTheme = false) { RepzAvatarShowcase() }
}

@Preview
@Composable
private fun RepzAvatarDarkPreview() {
    RepzTheme(darkTheme = true) { RepzAvatarShowcase() }
}
