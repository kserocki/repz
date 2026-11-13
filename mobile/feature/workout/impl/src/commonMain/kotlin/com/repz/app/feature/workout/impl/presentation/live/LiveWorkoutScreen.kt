package com.repz.app.feature.workout.impl.presentation.live

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.repz.app.core.designsystem.component.RepzSnackbarHostState
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
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.seconds

/** TODO(REP-22): switch to the real undo-snackbar API once it exists; this is a stand-in
 * (6 s, matching MOT-24's undo window). */
private val UndoSnackbarDuration = 6.seconds

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
                    navigator.navigate(ExercisePickerRoute)
                }

                LiveWorkoutEffect.ShowSetDeleted -> {
                    scope.launch {
                        showUndoSnackbar(
                            snackbarHostState,
                            setDeletedMessage,
                            undoLabel,
                        ) { viewModel.onIntent(LiveWorkoutIntent.SetDeletionUndone) }
                    }
                }

                LiveWorkoutEffect.ShowExerciseRemoved -> {
                    scope.launch {
                        showUndoSnackbar(snackbarHostState, exerciseRemovedMessage, undoLabel) {
                            viewModel.onIntent(LiveWorkoutIntent.ExerciseRemovalUndone)
                        }
                    }
                }

                LiveWorkoutEffect.ShowFinishFailed -> {
                    scope.launch { snackbarHostState.show(finishFailedMessage) }
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

/** [com.repz.app.core.designsystem.component.RepzSnackbarHostState.showWithAction] bounded to
 * [UndoSnackbarDuration], then dismissed; `onUndo` fires only if the action was tapped within that
 * window. */
private suspend fun showUndoSnackbar(
    snackbarHostState: RepzSnackbarHostState,
    message: String,
    actionLabel: String,
    onUndo: () -> Unit,
) {
    val tapped = withTimeoutOrNull(UndoSnackbarDuration) { snackbarHostState.showWithAction(message, actionLabel) }
    snackbarHostState.dismissCurrent()
    if (tapped == true) onUndo()
}

@Preview
@Composable
private fun LiveWorkoutScreenDefaultPreview() {
    val snackbarHostState = rememberRepzSnackbarHostState()
    RepzTheme(darkTheme = false) {
        LiveWorkoutContent(state = LiveWorkoutUiState(isLoading = false), onIntent = {}, snackbarHostState = snackbarHostState)
    }
}
