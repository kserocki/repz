package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

@Composable
private fun RepzHeroStartButtonShowcase() {
    val startWorkoutLabel = "Start workout"
    val fromTemplateDescription = "Start from template"
    Column(modifier = Modifier.padding(RepzSpacing.lg), verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        RepzHeroStartButton(startWorkoutLabel, onClick = {}, modifier = Modifier.fillMaxWidth())
        RepzHeroStartButton(startWorkoutLabel, onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false)
        RepzHeroStartSplitButton(
            text = startWorkoutLabel,
            onClick = {},
            secondaryContentDescription = fromTemplateDescription,
            onSecondaryClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
        RepzHeroStartSplitButton(
            text = startWorkoutLabel,
            onClick = {},
            secondaryContentDescription = fromTemplateDescription,
            onSecondaryClick = {},
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
        )
    }
}

@Preview
@Composable
private fun RepzHeroStartButtonLightPreview() {
    RepzTheme(darkTheme = false) { RepzHeroStartButtonShowcase() }
}

@Preview
@Composable
private fun RepzHeroStartButtonDarkPreview() {
    RepzTheme(darkTheme = true) { RepzHeroStartButtonShowcase() }
}
