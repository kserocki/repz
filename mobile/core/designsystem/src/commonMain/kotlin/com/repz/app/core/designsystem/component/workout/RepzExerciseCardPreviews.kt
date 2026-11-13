package com.repz.app.core.designsystem.component.workout

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzTheme
import kotlinx.collections.immutable.persistentListOf

private val CardMenuItems =
    persistentListOf(
        RepzMenuItem(label = "Reorder", onClick = {}),
        RepzMenuItem(label = "Notes", onClick = {}),
        RepzMenuItem(label = "Remove", onClick = {}, isDestructive = true),
    )

private val TypeMenuItemsForCard = persistentListOf(RepzMenuItem(label = "Normal", onClick = {}, isChecked = true))

@Composable
private fun RepzExerciseCardSample(notes: String? = null) {
    RepzExerciseCard(
        name = "Bench Press",
        subtitle = "Chest · 3 of 4 done",
        notes = notes,
        isEditingNotes = false,
        notesLabel = "Notes",
        onNotesClick = {},
        onNotesChange = {},
        onNotesEditingFinish = {},
        menuItems = CardMenuItems,
        menuContentDescription = "Exercise options",
        onHeaderLongClick = {},
        columnLabels = RepzSetColumnLabels(set = "Set", previous = "Prev", weight = "kg", reps = "Reps"),
        addSetLabel = "Add set",
        isAddSetEnabled = true,
        onAddSetClick = {},
    ) { isPreviousColumnVisible ->
        CardSetRow(
            badgeLabel = "W",
            type = RepzSetType.Warmup,
            previousText = "60 × 10",
            weightText = "60",
            repsText = "10",
            isCompleted = true,
            isPreviousColumnVisible = isPreviousColumnVisible,
        )
        CardSetRow(
            badgeLabel = "1",
            previousText = "80 × 8",
            weightText = "80",
            repsText = "8",
            isCompleted = true,
            isPreviousColumnVisible = isPreviousColumnVisible,
        )
        CardSetRow(
            badgeLabel = "2",
            previousText = "80 × 8",
            weightText = "82.5",
            repsText = "8",
            isCompleted = true,
            isPreviousColumnVisible = isPreviousColumnVisible,
        )
        CardSetRow(
            badgeLabel = "3",
            previousText = "80 × 7",
            weightText = "82.5",
            repsText = "7",
            isPrefilled = true,
            isPreviousColumnVisible = isPreviousColumnVisible,
        )
    }
}

@Composable
private fun CardSetRow(
    badgeLabel: String,
    previousText: String?,
    weightText: String,
    repsText: String,
    isPreviousColumnVisible: Boolean,
    type: RepzSetType = RepzSetType.Normal,
    isPrefilled: Boolean = false,
    isCompleted: Boolean = false,
) {
    RepzSetRow(
        badgeLabel = badgeLabel,
        type = type,
        previousText = previousText,
        noPreviousText = "—",
        lastLineText = previousText?.let { "Last: $it" },
        weightText = weightText,
        repsText = repsText,
        isPrefilled = isPrefilled,
        editingField = null,
        isCompleted = isCompleted,
        isPreviousColumnVisible = isPreviousColumnVisible,
        typeMenuItems = TypeMenuItemsForCard,
        onWeightClick = {},
        onRepsClick = {},
        onCompleteClick = {},
        onDelete = {},
        accessibilityActions = persistentListOf(),
        completeContentDescription = "Complete set $badgeLabel, $weightText kilograms for $repsText reps",
        weightContentDescription = "Weight, set $badgeLabel",
        repsContentDescription = "Reps, set $badgeLabel",
        badgeContentDescription = "Set $badgeLabel type",
    )
}

@Preview
@Composable
private fun RepzExerciseCardLightPreview() {
    RepzTheme(darkTheme = false) { RepzExerciseCardSample() }
}

@Preview
@Composable
private fun RepzExerciseCardDarkPreview() {
    RepzTheme(darkTheme = true) { RepzExerciseCardSample() }
}

@Preview
@Composable
private fun RepzExerciseCardWithNotesLightPreview() {
    RepzTheme(darkTheme = false) { RepzExerciseCardSample(notes = "Pause on chest in the last set.") }
}

@Preview
@Composable
private fun RepzExerciseCardWithNotesDarkPreview() {
    RepzTheme(darkTheme = true) { RepzExerciseCardSample(notes = "Pause on chest in the last set.") }
}

@Preview(fontScale = 2f)
@Composable
private fun RepzExerciseCardFontScale2LightPreview() {
    RepzTheme(darkTheme = false) { RepzExerciseCardSample() }
}

@Preview(fontScale = 2f)
@Composable
private fun RepzExerciseCardFontScale2DarkPreview() {
    RepzTheme(darkTheme = true) { RepzExerciseCardSample() }
}
