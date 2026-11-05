package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

@Composable
private fun RepzListItemShowcase() {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        RepzListItem(headline = "Sync", onClick = {}, leadingIcon = RepzIcon.CloudOff)
        RepzListItem(
            headline = "Log out",
            onClick = {},
            leadingIcon = RepzIcon.Logout,
            tone = RepzListItemTone.Destructive,
        )
        RepzListItem(
            headline = "A label long enough to wrap onto a second line at this component's width",
            onClick = {},
            leadingIcon = RepzIcon.Error,
        )
    }
}

@Preview
@Composable
private fun RepzListItemLightPreview() {
    RepzTheme(darkTheme = false) { RepzListItemShowcase() }
}

@Preview
@Composable
private fun RepzListItemDarkPreview() {
    RepzTheme(darkTheme = true) { RepzListItemShowcase() }
}
