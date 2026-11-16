package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

// Board 04's own FAB labels, reused here as preview sample copy only.
private const val NEW_WORKOUT_LABEL = "New workout"

@Composable
private fun RepzFabShowcase() {
    Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        RepzFab(icon = RepzIcon.Add, contentDescription = NEW_WORKOUT_LABEL, onClick = {})
        RepzExtendedFab(text = NEW_WORKOUT_LABEL, icon = RepzIcon.Add, onClick = {})
    }
}

@Preview
@Composable
private fun RepzFabLightPreview() {
    RepzTheme(darkTheme = false) { RepzFabShowcase() }
}

@Preview
@Composable
private fun RepzFabDarkPreview() {
    RepzTheme(darkTheme = true) { RepzFabShowcase() }
}
