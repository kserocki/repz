package com.repz.app.feature.workout.impl.presentation.live

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.repz.app.core.domain.model.WorkoutSetId
import kotlinx.collections.immutable.ImmutableList

/** [LiveWorkoutKeypadFocusEffect] and [LiveWorkoutAutoScrollEffect] together, both keyed off the
 * keypad's own open state/target. */
@Composable
internal fun LiveWorkoutKeypadFocusAndScrollEffects(
    isKeypadOpen: Boolean,
    keypadTargetSetId: WorkoutSetId?,
    exercises: ImmutableList<LiveExerciseUi>,
    listState: LazyListState,
) {
    LiveWorkoutKeypadFocusEffect(isKeypadOpen)
    LiveWorkoutAutoScrollEffect(targetSetId = keypadTargetSetId, exercises = exercises, listState = listState)
}

/** Jumps, without animation, to the target set's card only when it isn't composed yet; the row's
 * own `bringIntoView` then positions it above the keypad. */
@Composable
private fun LiveWorkoutAutoScrollEffect(
    targetSetId: WorkoutSetId?,
    exercises: ImmutableList<LiveExerciseUi>,
    listState: LazyListState,
) {
    LaunchedEffect(targetSetId) {
        val setId = targetSetId ?: return@LaunchedEffect
        val exerciseIndex = exercises.indexOfFirst { exercise -> exercise.sets.any { it.id == setId } }
        val isAlreadyVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == exerciseIndex }
        if (exerciseIndex >= 0 && !isAlreadyVisible) listState.scrollToItem(exerciseIndex)
    }
}
