package com.repz.app.feature.workout.impl.presentation.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.repz.app.core.designsystem.component.rememberRepzSnackbarHostState
import com.repz.app.core.designsystem.haptics.RepzHaptics
import com.repz.app.core.designsystem.theme.RepzTheme
import com.repz.app.core.domain.model.WorkoutExerciseId
import com.repz.app.core.navigation.Navigator
import com.repz.app.feature.workout.api.ExercisePickerRoute
import com.repz.app.feature.workout.impl.generated.resources.Res
import com.repz.app.feature.workout.impl.generated.resources.picker_selection_limit
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getPluralString
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.uuid.Uuid

/** Stateful (ADR-0005): resolves [ExercisePickerViewModel] itself, keyed by [route]'s Replace target
 * if any, and forwards [ExercisePickerEffect.Close] (added/replaced, closed, or no active workout)
 * to [navigator]. The rest of the per-state previews live in the other `ExercisePicker*Previews.kt`
 * files. */
@Composable
fun ExercisePickerScreen(
    navigator: Navigator,
    route: ExercisePickerRoute,
    modifier: Modifier = Modifier,
) {
    val replacingWorkoutExerciseId = route.replacingWorkoutExerciseId?.let { WorkoutExerciseId(Uuid.parse(it)) }
    val viewModel = koinViewModel<ExercisePickerViewModel> { parametersOf(replacingWorkoutExerciseId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = rememberRepzSnackbarHostState()
    val haptics = RepzHaptics.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ExercisePickerEffect.Close -> {
                    navigator.goBack()
                }

                ExercisePickerEffect.PlaySelectionHaptic -> {
                    haptics.tick()
                }

                ExercisePickerEffect.PlayConfirmHaptic -> {
                    haptics.confirm()
                }

                ExercisePickerEffect.PlayRejectHaptic -> {
                    haptics.reject()
                }

                is ExercisePickerEffect.ShowSelectionLimit -> {
                    scope.launch {
                        val message = getPluralString(Res.plurals.picker_selection_limit, effect.maxExercises, effect.maxExercises)
                        snackbarHostState.showMessage(message)
                    }
                }
            }
        }
    }

    ExercisePickerContent(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun ExercisePickerScreenDefaultPreview() {
    val snackbarHostState = rememberRepzSnackbarHostState()
    RepzTheme(darkTheme = false) {
        ExercisePickerContent(state = ExercisePickerAddDefaultState, onIntent = {}, snackbarHostState = snackbarHostState)
    }
}
