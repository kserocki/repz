package com.repz.app.feature.workout.impl.presentation.live

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.domain.model.Muscle
import com.repz.app.core.domain.model.Rpe
import com.repz.app.core.domain.model.SetType
import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.preferences.WeightStep
import kotlinx.collections.immutable.persistentListOf

private val KeypadWeightTarget =
    LiveKeypadTargetUi(
        setId = BenchPressSets[3].id,
        field = SetField.Weight,
        exerciseName = "Bench Press",
        normalSetNumber = 3,
        setType = SetType.NORMAL,
        current = LiveSetValuesUi(weight = BenchPressSets[3].weight, reps = BenchPressSets[3].reps, rpe = null as Rpe?),
        last = LiveSetValuesUi(weight = Weight.fromKilograms(82.5), reps = 8, rpe = null),
        isLastSetOfWorkout = false,
    )

internal val LiveWorkoutFinishDisabledState =
    LiveWorkoutInProgressState.copy(
        exercises =
            persistentListOf(
                previewExercise(
                    seed = 12,
                    name = "Bench Press",
                    muscle = Muscle.CHEST,
                    sets = listOf(previewSet(seed = 8, normalSetNumber = 1)),
                ),
            ),
        canFinish = false,
    )

internal val LiveWorkoutKeypadWeightState =
    LiveWorkoutInProgressState.copy(
        editingField = SetFieldRef(KeypadWeightTarget.setId, SetField.Weight),
        keypadTarget = KeypadWeightTarget,
        weightStep = WeightStep.defaultFor(UnitSystem.KG),
    )

internal val LiveWorkoutKeypadRepsState =
    LiveWorkoutInProgressState.copy(
        editingField = SetFieldRef(KeypadWeightTarget.setId, SetField.Reps),
        keypadTarget = KeypadWeightTarget.copy(field = SetField.Reps),
        weightStep = WeightStep.defaultFor(UnitSystem.KG),
    )

@Preview(locale = "pl")
@Composable
private fun LiveWorkoutInProgressPolishPreview() = LiveWorkoutPreview(darkTheme = false, state = LiveWorkoutInProgressState)

@Preview(fontScale = 2f)
@Composable
private fun LiveWorkoutInProgressFontScale2Preview() = LiveWorkoutPreview(darkTheme = false, state = LiveWorkoutInProgressState)

@Preview
@Composable
private fun LiveWorkoutFinishDisabledLightPreview() = LiveWorkoutPreview(darkTheme = false, state = LiveWorkoutFinishDisabledState)

@Preview
@Composable
private fun LiveWorkoutFinishDisabledDarkPreview() = LiveWorkoutPreview(darkTheme = true, state = LiveWorkoutFinishDisabledState)

@Preview
@Composable
private fun LiveWorkoutKeypadWeightLightPreview() = LiveWorkoutPreview(darkTheme = false, state = LiveWorkoutKeypadWeightState)

@Preview
@Composable
private fun LiveWorkoutKeypadWeightDarkPreview() = LiveWorkoutPreview(darkTheme = true, state = LiveWorkoutKeypadWeightState)

@Preview
@Composable
private fun LiveWorkoutKeypadRepsLightPreview() = LiveWorkoutPreview(darkTheme = false, state = LiveWorkoutKeypadRepsState)

@Preview
@Composable
private fun LiveWorkoutKeypadRepsDarkPreview() = LiveWorkoutPreview(darkTheme = true, state = LiveWorkoutKeypadRepsState)
