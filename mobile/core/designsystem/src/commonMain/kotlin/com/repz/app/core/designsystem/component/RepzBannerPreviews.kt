package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

@Composable
private fun RepzBannerShowcase() {
    val titleOnly = "You need a connection to create an account"
    val withBodyTitle = "You're offline"
    val withBody = "Logging in needs a connection. Once you're in, Repz works offline."
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.md)) {
        RepzBanner(icon = RepzIcon.CloudOff, title = titleOnly)
        RepzBanner(icon = RepzIcon.CloudOff, title = withBodyTitle, body = withBody)
    }
}

@Preview
@Composable
private fun RepzBannerLightPreview() {
    RepzTheme(darkTheme = false) { RepzBannerShowcase() }
}

@Preview
@Composable
private fun RepzBannerDarkPreview() {
    RepzTheme(darkTheme = true) { RepzBannerShowcase() }
}
