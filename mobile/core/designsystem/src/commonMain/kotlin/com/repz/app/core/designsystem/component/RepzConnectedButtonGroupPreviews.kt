package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme
import kotlinx.collections.immutable.persistentListOf

@Composable
private fun RepzConnectedButtonGroupShowcase() {
    var unitIndex by remember { mutableIntStateOf(0) }
    val units = persistentListOf("kg", "lb")
    val modes = persistentListOf("Weight", "Reps", "RPE")

    Column(modifier = Modifier.padding(RepzSpacing.lg), verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        RepzConnectedButtonGroup(options = units, selectedIndex = unitIndex, onSelectedIndexChange = { unitIndex = it })
        RepzConnectedButtonGroup(
            options = modes,
            selectedIndex = 1,
            onSelectedIndexChange = {},
            modifier = Modifier.fillMaxWidth(),
        )
        RepzConnectedButtonGroup(options = units, selectedIndex = 0, onSelectedIndexChange = {}, enabled = false)
    }
}

@Preview
@Composable
private fun RepzConnectedButtonGroupLightPreview() {
    RepzTheme(darkTheme = false) { RepzConnectedButtonGroupShowcase() }
}

@Preview
@Composable
private fun RepzConnectedButtonGroupDarkPreview() {
    RepzTheme(darkTheme = true) { RepzConnectedButtonGroupShowcase() }
}
