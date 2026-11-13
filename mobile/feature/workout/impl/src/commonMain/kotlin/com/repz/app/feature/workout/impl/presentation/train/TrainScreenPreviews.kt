package com.repz.app.feature.workout.impl.presentation.train

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzTheme
import com.repz.app.core.domain.model.WorkoutTemplateId
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.DayOfWeek
import kotlin.uuid.Uuid

/** Shared by this file (both in this package, `internal` so no import is needed); `TrainScreen.kt`
 * keeps its own single preview for Konsist N4. */
@Composable
internal fun TrainPreview(
    darkTheme: Boolean,
    state: TrainUiState,
) {
    RepzTheme(darkTheme = darkTheme) {
        TrainContent(state = state, onIntent = {})
    }
}

internal val TrainLoadingState = TrainUiState(isLoading = true)
internal val TrainStartState = TrainUiState(isLoading = false, today = DayOfWeek.WEDNESDAY)
internal val TrainResumeState =
    TrainUiState(isLoading = false, today = DayOfWeek.WEDNESDAY, activeWorkoutName = "Push A")
internal val TrainWithTemplatesState =
    TrainUiState(
        isLoading = false,
        today = DayOfWeek.WEDNESDAY,
        templates =
            persistentListOf(
                TemplateItemUi(
                    id = WorkoutTemplateId(Uuid.parse("00000000-0000-0000-0000-000000000001")),
                    name = "Push A",
                    exerciseCount = 5,
                ),
                TemplateItemUi(
                    id = WorkoutTemplateId(Uuid.parse("00000000-0000-0000-0000-000000000002")),
                    name = "Pull A",
                    exerciseCount = 1,
                ),
            ),
    )

@Preview
@Composable
private fun TrainLoadingLightPreview() = TrainPreview(darkTheme = false, state = TrainLoadingState)

@Preview
@Composable
private fun TrainLoadingDarkPreview() = TrainPreview(darkTheme = true, state = TrainLoadingState)

@Preview
@Composable
private fun TrainStartLightPreview() = TrainPreview(darkTheme = false, state = TrainStartState)

@Preview
@Composable
private fun TrainStartDarkPreview() = TrainPreview(darkTheme = true, state = TrainStartState)

@Preview
@Composable
private fun TrainResumeLightPreview() = TrainPreview(darkTheme = false, state = TrainResumeState)

@Preview
@Composable
private fun TrainResumeDarkPreview() = TrainPreview(darkTheme = true, state = TrainResumeState)

@Preview
@Composable
private fun TrainWithTemplatesLightPreview() = TrainPreview(darkTheme = false, state = TrainWithTemplatesState)

@Preview
@Composable
private fun TrainWithTemplatesDarkPreview() = TrainPreview(darkTheme = true, state = TrainWithTemplatesState)

@Preview(locale = "pl")
@Composable
private fun TrainWithTemplatesPolishPreview() = TrainPreview(darkTheme = false, state = TrainWithTemplatesState)

@Preview(fontScale = 2f)
@Composable
private fun TrainFontScale2Preview() = TrainPreview(darkTheme = false, state = TrainWithTemplatesState)
