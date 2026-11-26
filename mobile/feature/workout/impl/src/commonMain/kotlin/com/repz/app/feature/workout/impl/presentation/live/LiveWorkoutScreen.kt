package com.repz.app.feature.workout.impl.presentation.live

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
import com.repz.app.core.navigation.Navigator
import com.repz.app.feature.workout.api.ExercisePickerRoute
import com.repz.app.feature.workout.impl.generated.resources.Res
import com.repz.app.feature.workout.impl.generated.resources.exercise_removed
import com.repz.app.feature.workout.impl.generated.resources.finish_failed
import com.repz.app.feature.workout.impl.generated.resources.set_deleted
import com.repz.app.feature.workout.impl.generated.resources.undo
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Stateful (ADR-0005): resolves [LiveWorkoutViewModel] itself, forwards navigation
 * ([LiveWorkoutEffect.Close]/[LiveWorkoutEffect.OpenExercisePicker]) to [navigator], and shows the
 * undo/failure snackbars and haptics for the rest. The per-state previews live in
 * `LiveWorkoutScreenPreviews.kt`. */
@Composable
fun LiveWorkoutScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<LiveWorkoutViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = rememberRepzSnackbarHostState()
    val haptics = RepzHaptics.current
    val scope = rememberCoroutineScope()

    val setDeletedMessage = stringResource(Res.string.set_deleted)
    val exerciseRemovedMessage = stringResource(Res.string.exercise_removed)
    val finishFailedMessage = stringResource(Res.string.finish_failed)
    val undoLabel = stringResource(Res.string.undo)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                LiveWorkoutEffect.Close -> {
                    navigator.goBack()
                }

                LiveWorkoutEffect.OpenExercisePicker -> {
                    navigator.navigate(ExercisePickerRoute())
                }

                is LiveWorkoutEffect.OpenExerciseReplacement -> {
                    navigator.navigate(ExercisePickerRoute(effect.exerciseId.value.toString()))
                }

                LiveWorkoutEffect.ShowSetDeleted -> {
                    scope.launch {
                        val undone = snackbarHostState.showUndo(setDeletedMessage, undoLabel)
                        if (undone) viewModel.onIntent(LiveWorkoutIntent.SetDeletionUndone)
                    }
                }

                LiveWorkoutEffect.ShowExerciseRemoved -> {
                    scope.launch {
                        val undone = snackbarHostState.showUndo(exerciseRemovedMessage, undoLabel)
                        if (undone) viewModel.onIntent(LiveWorkoutIntent.ExerciseRemovalUndone)
                    }
                }

                LiveWorkoutEffect.ShowFinishFailed -> {
                    scope.launch { snackbarHostState.showMessage(finishFailedMessage) }
                }

                LiveWorkoutEffect.PlayConfirmHaptic -> {
                    haptics.confirm()
                }

                LiveWorkoutEffect.PlayRejectHaptic -> {
                    haptics.reject()
                }
            }
        }
    }

    LiveWorkoutContent(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun LiveWorkoutScreenDefaultPreview() {
    val snackbarHostState = rememberRepzSnackbarHostState()
    RepzTheme(darkTheme = false) {
        LiveWorkoutContent(state = LiveWorkoutUiState(isLoading = false), onIntent = {}, snackbarHostState = snackbarHostState)
    }
}
