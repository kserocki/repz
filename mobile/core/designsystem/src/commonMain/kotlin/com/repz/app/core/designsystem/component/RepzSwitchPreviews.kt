package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

@Composable
private fun RepzSwitchShowcase() {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.md)) {
        RepzSwitch(checked = true, onCheckedChange = {})
        RepzSwitch(checked = false, onCheckedChange = {})
        RepzSwitch(checked = true, onCheckedChange = {}, enabled = false)
        RepzSwitch(checked = false, onCheckedChange = {}, enabled = false)
        RepzSwitch(checked = true, onCheckedChange = null)
    }
}

@Preview
@Composable
private fun RepzSwitchLightPreview() {
    RepzTheme(darkTheme = false) { RepzSwitchShowcase() }
}

@Preview
@Composable
private fun RepzSwitchDarkPreview() {
    RepzTheme(darkTheme = true) { RepzSwitchShowcase() }
}
