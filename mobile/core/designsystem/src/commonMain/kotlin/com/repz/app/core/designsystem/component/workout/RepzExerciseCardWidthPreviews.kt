package com.repz.app.core.designsystem.component.workout

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzTheme
import kotlinx.collections.immutable.persistentListOf

private val WidthPreviewMenuItems =
    persistentListOf(
        RepzMenuItem(label = "Reorder", onClick = {}),
        RepzMenuItem(label = "Notes", onClick = {}),
        RepzMenuItem(label = "Remove", onClick = {}, isDestructive = true),
    )

/** Demonstrates the header and its rows agreeing on the Previous column at the narrow-width
 * threshold (DESIGN_SYSTEM §9 `SetRow`): hidden at 320 dp, shown again right at 360 dp. */
@Composable
private fun RepzExerciseCardWidthSample() {
    RepzExerciseCard(
        name = "Bench Press",
        subtitle = "Chest · 1 of 3 done",
        notes = null,
        isEditingNotes = false,
        notesLabel = "Notes",
        onNotesClick = {},
        onNotesChange = {},
        onNotesEditingFinish = {},
        menuItems = WidthPreviewMenuItems,
        menuContentDescription = "Exercise options",
        onHeaderLongClick = {},
        columnLabels = RepzSetColumnLabels(set = "Set", previous = "Prev", weight = "kg", reps = "Reps"),
        addSetLabel = "Add set",
        isAddSetEnabled = true,
        onAddSetClick = {},
    ) { isPreviousColumnVisible ->
        RepzSetRow(
            badgeLabel = "1",
            type = RepzSetType.Normal,
            previousText = "80 × 8",
            noPreviousText = "—",
            lastLineText = "Last: 80 × 8",
            weightText = "80",
            repsText = "8",
            isPrefilled = false,
            editingField = null,
            isCompleted = true,
            isPreviousColumnVisible = isPreviousColumnVisible,
            typeMenuItems = persistentListOf(),
            onWeightClick = {},
            onRepsClick = {},
            onCompleteClick = {},
            onDelete = {},
            accessibilityActions = persistentListOf(),
            completeContentDescription = "Complete set 1, 80 kilograms for 8 reps",
            weightContentDescription = "Weight, set 1",
            repsContentDescription = "Reps, set 1",
            badgeContentDescription = "Set 1 type",
        )
    }
}

@Preview(widthDp = 320)
@Composable
private fun RepzExerciseCardNarrowLightPreview() {
    RepzTheme(darkTheme = false) { RepzExerciseCardWidthSample() }
}

@Preview(widthDp = 320)
@Composable
private fun RepzExerciseCardNarrowDarkPreview() {
    RepzTheme(darkTheme = true) { RepzExerciseCardWidthSample() }
}

@Preview(widthDp = 360)
@Composable
private fun RepzExerciseCardThresholdLightPreview() {
    RepzTheme(darkTheme = false) { RepzExerciseCardWidthSample() }
}

@Preview(widthDp = 360)
@Composable
private fun RepzExerciseCardThresholdDarkPreview() {
    RepzTheme(darkTheme = true) { RepzExerciseCardWidthSample() }
}
