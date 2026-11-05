package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

@Composable
private fun RepzTopAppBarShowcase() {
    val sampleTitle = "Create your account"
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        RepzTopAppBar()
        RepzTopAppBar(title = sampleTitle)
        RepzTopAppBar(title = sampleTitle, onBackClick = {})
    }
}

@Preview
@Composable
private fun RepzTopAppBarLightPreview() {
    RepzTheme(darkTheme = false) { RepzTopAppBarShowcase() }
}

@Preview
@Composable
private fun RepzTopAppBarDarkPreview() {
    RepzTheme(darkTheme = true) { RepzTopAppBarShowcase() }
}
