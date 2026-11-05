package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

private const val WELCOME_PAGE_COUNT = 3

@Composable
private fun RepzPageIndicatorShowcase() {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.md)) {
        for (page in 0 until WELCOME_PAGE_COUNT) {
            val description = "Page ${page + 1} of $WELCOME_PAGE_COUNT"
            RepzPageIndicator(pageCount = WELCOME_PAGE_COUNT, currentPage = page, contentDescription = description)
        }
    }
}

@Preview
@Composable
private fun RepzPageIndicatorLightPreview() {
    RepzTheme(darkTheme = false) { RepzPageIndicatorShowcase() }
}

@Preview
@Composable
private fun RepzPageIndicatorDarkPreview() {
    RepzTheme(darkTheme = true) { RepzPageIndicatorShowcase() }
}
