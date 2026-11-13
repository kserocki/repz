package com.repz.app.feature.workout.impl.presentation.live

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.repz.app.core.designsystem.component.RepzButton
import com.repz.app.core.designsystem.component.RepzButtonSize
import com.repz.app.core.designsystem.component.RepzButtonStyle
import com.repz.app.core.designsystem.component.RepzTopAppBar
import com.repz.app.core.designsystem.component.workout.RepzAccessibilityAction
import com.repz.app.core.designsystem.component.workout.RepzExerciseReorderList
import com.repz.app.core.designsystem.component.workout.RepzReorderItem
import com.repz.app.core.designsystem.haptics.RepzHaptics
import com.repz.app.core.domain.model.WorkoutExerciseId
import com.repz.app.feature.workout.impl.generated.resources.Res
import com.repz.app.feature.workout.impl.generated.resources.exercise_unknown
import com.repz.app.feature.workout.impl.generated.resources.reorder_done
import com.repz.app.feature.workout.impl.generated.resources.reorder_drag_handle
import com.repz.app.feature.workout.impl.generated.resources.reorder_move_down
import com.repz.app.feature.workout.impl.generated.resources.reorder_move_up
import com.repz.app.feature.workout.impl.generated.resources.reorder_subtitle
import com.repz.app.feature.workout.impl.generated.resources.reorder_title
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Reorder mode (board 09 §10b, menu or a card header long-press): the header is replaced by a
 * plain [RepzTopAppBar] with a text "Done" action, and the card list by [RepzExerciseReorderList]
 * wrapped in its own scroll (the list itself doesn't scroll). Only `*Screen`/
 * `*Content` may know [LiveWorkoutIntent] (AGENTS.md §4, Konsist N8), so every callback here is
 * explicit.
 */
@Composable
internal fun LiveWorkoutReorderSection(
    exercises: ImmutableList<LiveExerciseUi>,
    onDoneClick: () -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = RepzHaptics.current

    Column(modifier = modifier.fillMaxSize()) {
        RepzTopAppBar(
            title = stringResource(Res.string.reorder_title),
            actions = {
                RepzButton(
                    text = stringResource(Res.string.reorder_done),
                    onClick = onDoneClick,
                    modifier = Modifier.testTag("reorder_done"),
                    style = RepzButtonStyle.Text,
                    size = RepzButtonSize.Small,
                )
            },
        )
        RepzExerciseReorderList(
            items = reorderItems(exercises, onMove),
            onMove = onMove,
            onDragStart = { haptics.dragStart() },
            onDragEnd = { haptics.dragEnd() },
            dragHandleContentDescription = stringResource(Res.string.reorder_drag_handle),
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            rowModifier = { index -> Modifier.testTag("reorder_row_$index") },
        )
    }
}

@Composable
private fun reorderItems(
    exercises: ImmutableList<LiveExerciseUi>,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
): ImmutableList<RepzReorderItem<WorkoutExerciseId>> {
    val unknownExerciseName = stringResource(Res.string.exercise_unknown)
    val moveUpLabel = stringResource(Res.string.reorder_move_up)
    val moveDownLabel = stringResource(Res.string.reorder_move_down)
    val lastIndex = exercises.lastIndex

    return exercises
        .mapIndexed { index, exercise ->
            RepzReorderItem(
                key = exercise.id,
                title = exercise.name ?: unknownExerciseName,
                subtitle =
                    pluralStringResource(
                        Res.plurals.reorder_subtitle,
                        exercise.sets.size,
                        exercise.sets.size,
                        exercise.completedSetCount,
                    ),
                isDone = exercise.sets.isNotEmpty() && exercise.completedSetCount == exercise.sets.size,
                moveUp = if (index > 0) RepzAccessibilityAction(moveUpLabel) { onMove(index, index - 1) } else null,
                moveDown = if (index < lastIndex) RepzAccessibilityAction(moveDownLabel) { onMove(index, index + 1) } else null,
            )
        }.toImmutableList()
}
