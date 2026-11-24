package com.repz.app.feature.workout.impl.presentation.picker

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun ExercisePickerSearchingLightPreview() = ExercisePickerPreview(darkTheme = false, state = ExercisePickerSearchingState)

@Preview
@Composable
private fun ExercisePickerSearchingDarkPreview() = ExercisePickerPreview(darkTheme = true, state = ExercisePickerSearchingState)

@Preview
@Composable
private fun ExercisePickerSelectedLightPreview() = ExercisePickerPreview(darkTheme = false, state = ExercisePickerSelectedState)

@Preview
@Composable
private fun ExercisePickerSelectedDarkPreview() = ExercisePickerPreview(darkTheme = true, state = ExercisePickerSelectedState)

@Preview
@Composable
private fun ExercisePickerReplaceLightPreview() = ExercisePickerPreview(darkTheme = false, state = ExercisePickerReplaceState)

@Preview
@Composable
private fun ExercisePickerReplaceDarkPreview() = ExercisePickerPreview(darkTheme = true, state = ExercisePickerReplaceState)

@Preview
@Composable
private fun ExercisePickerReplaceDialogLightPreview() = ExercisePickerPreview(darkTheme = false, state = ExercisePickerReplaceDialogState)

@Preview
@Composable
private fun ExercisePickerReplaceDialogDarkPreview() = ExercisePickerPreview(darkTheme = true, state = ExercisePickerReplaceDialogState)
