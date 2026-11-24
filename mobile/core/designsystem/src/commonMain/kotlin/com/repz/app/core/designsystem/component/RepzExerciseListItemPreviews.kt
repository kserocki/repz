package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

@Composable
private fun RepzExerciseListItemShowcase() {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.xs)) {
        RepzExerciseListItem(name = "Bench Press", supportingText = "Chest · Barbell", onClick = {})
        RepzExerciseListItem(name = "Barbell Row", supportingText = "Back · Barbell", onClick = {}, isSelected = false)
        RepzExerciseListItem(name = "Goblet Squat", supportingText = "Quads · Dumbbell", onClick = {}, isSelected = true)
    }
}

@Preview
@Composable
private fun RepzExerciseListItemLightPreview() {
    RepzTheme(darkTheme = false) { RepzExerciseListItemShowcase() }
}

@Preview
@Composable
private fun RepzExerciseListItemDarkPreview() {
    RepzTheme(darkTheme = true) { RepzExerciseListItemShowcase() }
}

// A long PL name and supporting text on every variant: both wrap, never truncate (AGENTS.md §4).
@Composable
private fun RepzExerciseListItemLongLabelShowcase() {
    val longName = "Podnoszenie sztangi ze stania przy ścianie"
    val longMeta = "Barki · Hantle"
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.xs)) {
        RepzExerciseListItem(name = longName, supportingText = longMeta, onClick = {})
        RepzExerciseListItem(name = longName, supportingText = longMeta, onClick = {}, isSelected = true)
    }
}

@Preview
@Composable
private fun RepzExerciseListItemLongLabelLightPreview() {
    RepzTheme(darkTheme = false) { RepzExerciseListItemLongLabelShowcase() }
}

@Preview
@Composable
private fun RepzExerciseListItemLongLabelDarkPreview() {
    RepzTheme(darkTheme = true) { RepzExerciseListItemLongLabelShowcase() }
}

@Preview(fontScale = 2f)
@Composable
private fun RepzExerciseListItemFontScale200LightPreview() {
    RepzTheme(darkTheme = false) { RepzExerciseListItemShowcase() }
}

@Preview(fontScale = 2f)
@Composable
private fun RepzExerciseListItemFontScale200DarkPreview() {
    RepzTheme(darkTheme = true) { RepzExerciseListItemShowcase() }
}
