package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

@Composable
private fun RepzSplitButtonShowcase() {
    val finishLabel = "Finish"
    val menuDescription = "Finish options"
    Column(modifier = Modifier.padding(RepzSpacing.lg), verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        RepzSplitButton(
            text = finishLabel,
            onClick = {},
            onTrailingClick = {},
            trailingContentDescription = menuDescription,
        )
        RepzSplitButton(
            text = finishLabel,
            onClick = {},
            onTrailingClick = {},
            trailingContentDescription = menuDescription,
            enabled = false,
        )
    }
}

@Preview
@Composable
private fun RepzSplitButtonLightPreview() {
    RepzTheme(darkTheme = false) { RepzSplitButtonShowcase() }
}

@Preview
@Composable
private fun RepzSplitButtonDarkPreview() {
    RepzTheme(darkTheme = true) { RepzSplitButtonShowcase() }
}
