package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

// Reproduces the split button's unbounded-height parent (the catalog's own verticalScroll), so a
// regression in heightIn(min = HeroHeight) + fillMaxHeight() renders visibly here.
@Composable
private fun RepzHeroStartSplitButtonInScrollingColumnShowcase() {
    val startWorkoutLabel = "Start workout"
    val wrappingLabel = "Start a very long workout name that wraps onto two lines"
    val fromTemplateDescription = "Start from template"
    Column(
        modifier = Modifier.height(240.dp).verticalScroll(rememberScrollState()).padding(RepzSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg),
    ) {
        RepzHeroStartSplitButton(
            text = startWorkoutLabel,
            onClick = {},
            secondaryContentDescription = fromTemplateDescription,
            onSecondaryClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
        RepzHeroStartSplitButton(
            text = wrappingLabel,
            onClick = {},
            secondaryContentDescription = fromTemplateDescription,
            onSecondaryClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview
@Composable
private fun RepzHeroStartSplitButtonInScrollingColumnLightPreview() {
    RepzTheme(darkTheme = false) { RepzHeroStartSplitButtonInScrollingColumnShowcase() }
}

@Preview
@Composable
private fun RepzHeroStartSplitButtonInScrollingColumnDarkPreview() {
    RepzTheme(darkTheme = true) { RepzHeroStartSplitButtonInScrollingColumnShowcase() }
}
