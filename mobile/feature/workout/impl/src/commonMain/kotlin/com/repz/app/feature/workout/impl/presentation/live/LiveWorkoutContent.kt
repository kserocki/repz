package com.repz.app.feature.workout.impl.presentation.live

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.repz.app.core.designsystem.component.RepzButton
import com.repz.app.core.designsystem.component.RepzButtonSize
import com.repz.app.core.designsystem.component.RepzButtonStyle
import com.repz.app.core.designsystem.component.RepzSnackbarHost
import com.repz.app.core.designsystem.component.RepzSnackbarHostState
import com.repz.app.core.designsystem.component.workout.RepzLiveWorkoutScaffold
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.domain.model.Rpe
import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.model.WorkoutSetId
import com.repz.app.core.domain.preferences.WeightStep
import com.repz.app.core.ui.keypad.SetValueEdit
import com.repz.app.core.ui.keypad.SetValueKeypad
import com.repz.app.core.ui.time.formatElapsed
import com.repz.app.feature.workout.impl.generated.resources.Res
import com.repz.app.feature.workout.impl.generated.resources.exercise_unknown
import com.repz.app.feature.workout.impl.generated.resources.keypad_context
import com.repz.app.feature.workout.impl.generated.resources.live_add_exercise
import org.jetbrains.compose.resources.stringResource

/**
 * The live workout (C10, board 09): [RepzLiveWorkoutScaffold] in normal mode, or
 * [LiveWorkoutReorderSection] while [LiveWorkoutUiState.isReordering]. The keypad
 * ([com.repz.app.core.ui.keypad.SetValueKeypad]) and the discard dialog both float above
 * everything else, as siblings of the scaffold in the outer [Box]. No MOT-xx animation ships here
 * (Q11-A).
 *
 * This is the only composable allowed to know [LiveWorkoutIntent] (AGENTS.md §4, Konsist N8): it
 * translates every [state] field and [onIntent] call into the explicit values and present-tense
 * callbacks every other composable below it takes instead, which is why its own body stays one
 * long function rather than being split into named helpers that would each need `state`/`onIntent`
 * again.
 */
@Suppress("LongMethod")
@Composable
fun LiveWorkoutContent(
    state: LiveWorkoutUiState,
    onIntent: (LiveWorkoutIntent) -> Unit,
    snackbarHostState: RepzSnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (state.isReordering) {
            LiveWorkoutReorderSection(
                exercises = state.exercises,
                onDoneClick = { onIntent(LiveWorkoutIntent.ReorderFinished) },
                onMove = { fromIndex, toIndex -> onIntent(LiveWorkoutIntent.ExerciseMoved(fromIndex, toIndex)) },
            )
        } else {
            val listState = rememberLazyListState()
            val layoutDirection = LocalLayoutDirection.current
            var keypadHeightPx by remember { mutableIntStateOf(0) }
            val keypadHeightDp = with(LocalDensity.current) { keypadHeightPx.toDp() }
            val isKeypadOpen = state.keypadTarget != null

            LaunchedEffect(state.keypadTarget?.setId) {
                val targetSetId = state.keypadTarget?.setId ?: return@LaunchedEffect
                val exerciseIndex = state.exercises.indexOfFirst { exercise -> exercise.sets.any { it.id == targetSetId } }
                if (exerciseIndex >= 0) listState.animateScrollToItem(exerciseIndex)
            }

            Box(modifier = Modifier.fillMaxSize()) {
                RepzLiveWorkoutScaffold(
                    header = {
                        LiveWorkoutHeaderSection(
                            name = state.workoutName,
                            elapsedText = formatElapsed(state.elapsed),
                            isFinishEnabled = state.canFinish && !state.isFinishing,
                            isEditingName = state.isEditingName,
                            onNameClick = { onIntent(LiveWorkoutIntent.NameClicked) },
                            onNameChange = { name -> onIntent(LiveWorkoutIntent.NameChanged(name)) },
                            onNameEditingFinish = { onIntent(LiveWorkoutIntent.NameEditingFinished) },
                            onMinimizeClick = { onIntent(LiveWorkoutIntent.MinimizeClicked) },
                            onFinishClick = { onIntent(LiveWorkoutIntent.FinishClicked) },
                            onDiscardClick = { onIntent(LiveWorkoutIntent.DiscardClicked) },
                        )
                    },
                    bottomBar = {
                        if (!isKeypadOpen) {
                            LiveWorkoutBottomBar(
                                canAddExercise = state.canAddExercise,
                                onAddExerciseClick = { onIntent(LiveWorkoutIntent.AddExerciseClicked) },
                            )
                        }
                    },
                    snackbarHost = { RepzSnackbarHost(state = snackbarHostState) },
                ) { innerPadding ->
                    LiveWorkoutExerciseList(
                        isEmpty = state.isEmpty,
                        exercises = state.exercises,
                        unit = state.unit,
                        editingField = state.editingField,
                        onAddExercisesClick = { onIntent(LiveWorkoutIntent.AddExerciseClicked) },
                        onReorderClick = { onIntent(LiveWorkoutIntent.ReorderStarted) },
                        onNotesClick = { exerciseId -> onIntent(LiveWorkoutIntent.NotesClicked(exerciseId)) },
                        onNotesChange = { exerciseId, notes -> onIntent(LiveWorkoutIntent.NotesChanged(exerciseId, notes)) },
                        onNotesEditingFinish = { exerciseId -> onIntent(LiveWorkoutIntent.NotesEditingFinished(exerciseId)) },
                        onRemoveClick = { exerciseId -> onIntent(LiveWorkoutIntent.ExerciseRemoved(exerciseId)) },
                        onAddSetClick = { exerciseId -> onIntent(LiveWorkoutIntent.AddSetClicked(exerciseId)) },
                        onSetFieldClick = { setId, field -> onIntent(LiveWorkoutIntent.SetFieldClicked(setId, field)) },
                        onSetCompleteClick = { setId -> onIntent(LiveWorkoutIntent.SetCompletionToggled(setId)) },
                        onSetDeleteClick = { setId -> onIntent(LiveWorkoutIntent.SetDeleted(setId)) },
                        onSetTypeSelect = { setId, type -> onIntent(LiveWorkoutIntent.SetTypeSelected(setId, type)) },
                        listState = listState,
                        contentPadding = innerPadding.withExtraBottom(keypadHeightDp, layoutDirection),
                    )
                }

                if (isKeypadOpen) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) { detectTapGestures(onTap = { onIntent(LiveWorkoutIntent.KeypadDismissed) }) },
                    )
                }

                LiveWorkoutKeypad(
                    keypadTarget = state.keypadTarget,
                    unit = state.unit,
                    weightStep = state.weightStep,
                    onWeightEnter = { setId, weight -> onIntent(LiveWorkoutIntent.SetWeightEntered(setId, weight)) },
                    onRepsEnter = { setId, reps -> onIntent(LiveWorkoutIntent.SetRepsEntered(setId, reps)) },
                    onRpeEnter = { setId, rpe -> onIntent(LiveWorkoutIntent.SetRpeEntered(setId, rpe)) },
                    onFieldSelect = { field -> onIntent(LiveWorkoutIntent.KeypadFieldSelected(field)) },
                    onWeightStepChange = { step -> onIntent(LiveWorkoutIntent.WeightStepChanged(step)) },
                    onNext = { onIntent(LiveWorkoutIntent.NextFieldRequested) },
                    onDismiss = { onIntent(LiveWorkoutIntent.KeypadDismissed) },
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .onSizeChanged { size -> keypadHeightPx = size.height },
                )
            }
        }

        state.discardConfirmation?.let { confirmation ->
            LiveWorkoutDiscardDialog(
                confirmation = confirmation,
                onConfirm = { onIntent(LiveWorkoutIntent.DiscardConfirmed) },
                onDismiss = { onIntent(LiveWorkoutIntent.DiscardDismissed) },
            )
        }
    }

    LiveWorkoutBackHandling(
        isKeypadOpen = state.keypadTarget != null,
        isReordering = state.isReordering,
        onKeypadDismiss = { onIntent(LiveWorkoutIntent.KeypadDismissed) },
        onReorderExit = { onIntent(LiveWorkoutIntent.ReorderFinished) },
        onMinimize = { onIntent(LiveWorkoutIntent.MinimizeClicked) },
    )
}

/** Adds [extraBottom] (the measured keypad height while it's open) on top of the scaffold's own
 * bottom inset, so the last card never sits behind the sheet. */
private fun PaddingValues.withExtraBottom(
    extraBottom: Dp,
    layoutDirection: LayoutDirection,
): PaddingValues =
    PaddingValues(
        start = calculateStartPadding(layoutDirection),
        top = calculateTopPadding(),
        end = calculateEndPadding(layoutDirection),
        bottom = calculateBottomPadding() + RepzSpacing.lg + extraBottom,
    )

@Composable
private fun LiveWorkoutBottomBar(
    canAddExercise: Boolean,
    onAddExerciseClick: () -> Unit,
) {
    RepzButton(
        text = stringResource(Res.string.live_add_exercise),
        onClick = onAddExerciseClick,
        modifier = Modifier.testTag("live_add_exercise"),
        style = RepzButtonStyle.Filled,
        size = RepzButtonSize.Medium,
        enabled = canAddExercise,
    )
}

/** Maps [keypadTarget] to [SetValueKeypad]'s own request; `null` hides the sheet. Only
 * `*Screen`/`*Content` may know [LiveWorkoutIntent] (AGENTS.md §4, Konsist N8), so every callback
 * here takes the affected [WorkoutSetId] explicitly instead. */
@Composable
private fun LiveWorkoutKeypad(
    keypadTarget: LiveKeypadTargetUi?,
    unit: UnitSystem,
    weightStep: WeightStep,
    onWeightEnter: (WorkoutSetId, Weight?) -> Unit,
    onRepsEnter: (WorkoutSetId, Int?) -> Unit,
    onRpeEnter: (WorkoutSetId, Rpe?) -> Unit,
    onFieldSelect: (SetField) -> Unit,
    onWeightStepChange: (WeightStep) -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val request =
        keypadTarget?.let { target ->
            val exerciseName = target.exerciseName ?: stringResource(Res.string.exercise_unknown)
            val badge = badgeLabel(target.setType, target.normalSetNumber)
            val contextLabel = stringResource(Res.string.keypad_context, exerciseName, badge)
            target.toKeypadRequest(contextLabel, unit, weightStep)
        }

    SetValueKeypad(
        request = request,
        onValueChange = { edit ->
            keypadTarget?.setId?.let { setId ->
                when (edit) {
                    is SetValueEdit.Weight -> onWeightEnter(setId, edit.weight)
                    is SetValueEdit.Reps -> onRepsEnter(setId, edit.reps)
                    is SetValueEdit.Rpe -> onRpeEnter(setId, edit.rpe)
                }
            }
        },
        onFieldSelect = { field -> onFieldSelect(field.toSetField()) },
        onWeightStepChange = onWeightStepChange,
        onNext = onNext,
        onDismiss = onDismiss,
        modifier = modifier,
    )
}
