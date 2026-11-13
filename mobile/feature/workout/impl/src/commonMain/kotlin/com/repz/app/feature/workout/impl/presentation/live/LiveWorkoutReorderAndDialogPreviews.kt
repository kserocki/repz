package com.repz.app.feature.workout.impl.presentation.live

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

internal val LiveWorkoutReorderingState = LiveWorkoutInProgressState.copy(isReordering = true)

internal val LiveWorkoutDiscardDialogState =
    LiveWorkoutInProgressState.copy(discardConfirmation = DiscardConfirmationUi(workoutName = "Push A", completedSetCount = 5))

@Preview
@Composable
private fun LiveWorkoutReorderingLightPreview() = LiveWorkoutPreview(darkTheme = false, state = LiveWorkoutReorderingState)

@Preview
@Composable
private fun LiveWorkoutReorderingDarkPreview() = LiveWorkoutPreview(darkTheme = true, state = LiveWorkoutReorderingState)

@Preview
@Composable
private fun LiveWorkoutDiscardDialogLightPreview() = LiveWorkoutPreview(darkTheme = false, state = LiveWorkoutDiscardDialogState)

@Preview
@Composable
private fun LiveWorkoutDiscardDialogDarkPreview() = LiveWorkoutPreview(darkTheme = true, state = LiveWorkoutDiscardDialogState)

@Preview(locale = "pl")
@Composable
private fun LiveWorkoutDiscardDialogPolishPreview() = LiveWorkoutPreview(darkTheme = false, state = LiveWorkoutDiscardDialogState)
