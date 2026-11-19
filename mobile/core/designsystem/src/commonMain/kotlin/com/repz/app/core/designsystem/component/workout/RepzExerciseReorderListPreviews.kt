package com.repz.app.core.designsystem.component.workout

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzTheme
import kotlinx.collections.immutable.persistentListOf

private val ReorderItems =
    persistentListOf(
        RepzReorderItem(
            key = "bench-press",
            title = "Bench Press",
            subtitle = "4 sets · 3 done",
            isDone = false,
            moveUp = null,
            moveDown = RepzAccessibilityAction(label = "Move down", onAction = {}),
        ),
        RepzReorderItem(
            key = "overhead-press",
            title = "Overhead Press",
            subtitle = "3 sets",
            isDone = false,
            moveUp = RepzAccessibilityAction(label = "Move up", onAction = {}),
            moveDown = RepzAccessibilityAction(label = "Move down", onAction = {}),
        ),
        RepzReorderItem(
            key = "lateral-raise",
            title = "Lateral Raise",
            subtitle = "4 sets",
            isDone = true,
            moveUp = RepzAccessibilityAction(label = "Move up", onAction = {}),
            moveDown = RepzAccessibilityAction(label = "Move down", onAction = {}),
        ),
        RepzReorderItem(
            key = "incline-db-press",
            title = "Incline DB Press",
            subtitle = "3 sets",
            isDone = false,
            moveUp = RepzAccessibilityAction(label = "Move up", onAction = {}),
            moveDown = RepzAccessibilityAction(label = "Move down", onAction = {}),
        ),
        RepzReorderItem(
            key = "triceps-pushdown",
            title = "Triceps Pushdown",
            subtitle = "3 sets",
            isDone = false,
            moveUp = RepzAccessibilityAction(label = "Move up", onAction = {}),
            moveDown = null,
        ),
    )

@Composable
private fun RepzExerciseReorderListSample() {
    RepzExerciseReorderList(
        items = ReorderItems,
        onMove = { _, _ -> },
        onDragStart = {},
        onDragEnd = {},
        dragHandleContentDescription = "Drag to reorder",
    )
}

@Preview
@Composable
private fun RepzExerciseReorderListLightPreview() {
    RepzTheme(darkTheme = false) { RepzExerciseReorderListSample() }
}

@Preview
@Composable
private fun RepzExerciseReorderListDarkPreview() {
    RepzTheme(darkTheme = true) { RepzExerciseReorderListSample() }
}
