package com.repz.app.feature.workout.impl.presentation.picker

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun ExercisePickerNoMatchQueryLightPreview() = ExercisePickerPreview(darkTheme = false, state = ExercisePickerNoMatchQueryState)

@Preview
@Composable
private fun ExercisePickerNoMatchQueryDarkPreview() = ExercisePickerPreview(darkTheme = true, state = ExercisePickerNoMatchQueryState)

@Preview
@Composable
private fun ExercisePickerNoMatchFiltersLightPreview() = ExercisePickerPreview(darkTheme = false, state = ExercisePickerNoMatchFiltersState)

@Preview
@Composable
private fun ExercisePickerNoMatchFiltersDarkPreview() = ExercisePickerPreview(darkTheme = true, state = ExercisePickerNoMatchFiltersState)

@Preview
@Composable
private fun ExercisePickerNoExercisesLightPreview() = ExercisePickerPreview(darkTheme = false, state = ExercisePickerNoExercisesState)

@Preview
@Composable
private fun ExercisePickerNoExercisesDarkPreview() = ExercisePickerPreview(darkTheme = true, state = ExercisePickerNoExercisesState)

@Preview(locale = "pl")
@Composable
private fun ExercisePickerSelectedPolishPreview() = ExercisePickerPreview(darkTheme = false, state = ExercisePickerSelectedState)

@Preview(fontScale = 2f)
@Composable
private fun ExercisePickerSelectedFontScale2Preview() = ExercisePickerPreview(darkTheme = false, state = ExercisePickerSelectedState)
