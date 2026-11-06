package com.repz.app.feature.workout.impl.presentation.picker

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzTheme
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.Muscle
import kotlinx.collections.immutable.persistentListOf
import kotlin.uuid.Uuid

/** Shared by this file; `ExercisePickerScreen.kt` keeps its own single preview for Konsist N4. */
@Composable
internal fun ExercisePickerPreview(
    darkTheme: Boolean,
    state: ExercisePickerUiState,
) {
    RepzTheme(darkTheme = darkTheme) {
        ExercisePickerContent(state = state, onIntent = {}, onBackClick = {})
    }
}

internal val ExercisePickerLoadingState = ExercisePickerUiState(isLoading = true)
internal val ExercisePickerListState =
    ExercisePickerUiState(
        isLoading = false,
        exercises =
            persistentListOf(
                PickerExerciseUi(id = ExerciseId(Uuid.parse("00000000-0000-0000-0000-000000000001")), name = "Bench Press", primaryMuscle = Muscle.CHEST),
                PickerExerciseUi(
                    id = ExerciseId(Uuid.parse("00000000-0000-0000-0000-000000000002")),
                    name = "Overhead Press",
                    primaryMuscle = Muscle.SHOULDERS,
                ),
                PickerExerciseUi(id = ExerciseId(Uuid.parse("00000000-0000-0000-0000-000000000003")), name = "Leg Press", primaryMuscle = Muscle.QUADRICEPS),
            ),
    )

@Preview
@Composable
private fun ExercisePickerLoadingLightPreview() = ExercisePickerPreview(darkTheme = false, state = ExercisePickerLoadingState)

@Preview
@Composable
private fun ExercisePickerListLightPreview() = ExercisePickerPreview(darkTheme = false, state = ExercisePickerListState)

@Preview
@Composable
private fun ExercisePickerListDarkPreview() = ExercisePickerPreview(darkTheme = true, state = ExercisePickerListState)

@Preview(locale = "pl")
@Composable
private fun ExercisePickerListPolishPreview() = ExercisePickerPreview(darkTheme = false, state = ExercisePickerListState)

@Preview(fontScale = 2f)
@Composable
private fun ExercisePickerFontScale2Preview() = ExercisePickerPreview(darkTheme = false, state = ExercisePickerListState)
