package com.repz.app.feature.workout.impl.presentation.live

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
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
import com.repz.app.core.designsystem.component.RepzSnackbarHost
import com.repz.app.core.designsystem.component.RepzSnackbarHostState
import com.repz.app.core.designsystem.component.workout.RepzLiveWorkoutBottomBar
import com.repz.app.core.designsystem.component.workout.RepzLiveWorkoutScaffold
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.domain.model.Rpe
import com.repz.app.core.domain.model.SetType
import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.model.WorkoutExerciseId
import com.repz.app.core.domain.model.WorkoutSetId
import com.repz.app.core.domain.preferences.WeightStep
import com.repz.app.core.ui.keypad.SetValueEdit
import com.repz.app.core.ui.keypad.SetValueKeypad
import com.repz.app.core.ui.time.formatElapsed
import com.repz.app.feature.workout.impl.generated.resources.Res
import com.repz.app.feature.workout.impl.generated.resources.exercise_unknown
import com.repz.app.feature.workout.impl.generated.resources.keypad_context
import com.repz.app.feature.workout.impl.generated.resources.live_add_exercise
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration

/**
 * The live workout (board 09): [RepzLiveWorkoutScaffold] in normal mode, via
 * [LiveWorkoutActiveSection] (which also floats the keypad above it), or
 * [LiveWorkoutReorderSection] while [LiveWorkoutUiState.isReordering]; [LiveWorkoutOverlays] layers
 * the discard confirmation and back-press handling over either branch. No MOT-xx animation ships here.
 */
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
            LiveWorkoutActiveSection(
                workoutName = state.workoutName,
                elapsed = state.elapsed,
                isFinishEnabled = state.canFinish && !state.isFinishing,
                isEditingName = state.isEditingName,
                canAddExercise = state.canAddExercise,
                isEmpty = state.isEmpty,
                exercises = state.exercises,
                unit = state.unit,
                editingField = state.editingField,
                keypadTarget = state.keypadTarget,
                weightStep = state.weightStep,
                snackbarHostState = snackbarHostState,
                onNameClick = { onIntent(LiveWorkoutIntent.NameClicked) },
                onNameChange = { name -> onIntent(LiveWorkoutIntent.NameChanged(name)) },
                onNameEditingFinish = { onIntent(LiveWorkoutIntent.NameEditingFinished) },
                onMinimizeClick = rememberEndingFieldEditing { onIntent(LiveWorkoutIntent.MinimizeClicked) },
                onFinishClick = rememberEndingFieldEditing { onIntent(LiveWorkoutIntent.FinishClicked) },
                onDiscardClick = { onIntent(LiveWorkoutIntent.DiscardClicked) },
                onAddExerciseClick = { onIntent(LiveWorkoutIntent.AddExerciseClicked) },
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
                onWeightEnter = { setId, weight -> onIntent(LiveWorkoutIntent.SetWeightEntered(setId, weight)) },
                onRepsEnter = { setId, reps -> onIntent(LiveWorkoutIntent.SetRepsEntered(setId, reps)) },
                onRpeEnter = { setId, rpe -> onIntent(LiveWorkoutIntent.SetRpeEntered(setId, rpe)) },
                onKeypadFieldSelect = { field -> onIntent(LiveWorkoutIntent.KeypadFieldSelected(field)) },
                onWeightStepChange = { step -> onIntent(LiveWorkoutIntent.WeightStepChanged(step)) },
                onNextField = { onIntent(LiveWorkoutIntent.NextFieldRequested) },
                onKeypadDismiss = { onIntent(LiveWorkoutIntent.KeypadDismissed) },
            )
        }
    }

    LiveWorkoutOverlays(
        discardConfirmation = state.discardConfirmation,
        isKeypadOpen = state.keypadTarget != null,
        isReordering = state.isReordering,
        onDiscardConfirm = { onIntent(LiveWorkoutIntent.DiscardConfirmed) },
        onDiscardDismiss = { onIntent(LiveWorkoutIntent.DiscardDismissed) },
        onKeypadDismiss = { onIntent(LiveWorkoutIntent.KeypadDismissed) },
        onReorderExit = { onIntent(LiveWorkoutIntent.ReorderFinished) },
        onMinimize = rememberEndingFieldEditing { onIntent(LiveWorkoutIntent.MinimizeClicked) },
    )
}

/**
 * The discard confirmation (while [discardConfirmation] isn't `null`) and system back-press
 * handling, both rendered as siblings of the content [Box] since
 * [RepzAlertDialog][com.repz.app.core.designsystem.component.RepzAlertDialog] draws in its own window.
 */
@Composable
private fun LiveWorkoutOverlays(
    discardConfirmation: DiscardConfirmationUi?,
    isKeypadOpen: Boolean,
    isReordering: Boolean,
    onDiscardConfirm: () -> Unit,
    onDiscardDismiss: () -> Unit,
    onKeypadDismiss: () -> Unit,
    onReorderExit: () -> Unit,
    onMinimize: () -> Unit,
) {
    discardConfirmation?.let { confirmation ->
        LiveWorkoutDiscardDialog(confirmation = confirmation, onConfirm = onDiscardConfirm, onDismiss = onDiscardDismiss)
    }

    LiveWorkoutBackHandling(
        isKeypadOpen = isKeypadOpen,
        isReordering = isReordering,
        onKeypadDismiss = onKeypadDismiss,
        onReorderExit = onReorderExit,
        onMinimize = onMinimize,
    )
}

/**
 * [LiveWorkoutContent]'s non-reordering branch: the scaffold, the auto-scroll-to-editing-set
 * effect and the floating keypad, all sized against the keypad's own measured height. While the
 * keypad is open, a tap that reaches nothing else (board 09 frame 11's "tapping outside closes")
 * dismisses it instead of a blocking scrim, so fields, buttons and scrolling underneath still work.
 */
@Composable
private fun LiveWorkoutActiveSection(
    workoutName: String,
    elapsed: Duration,
    isFinishEnabled: Boolean,
    isEditingName: Boolean,
    canAddExercise: Boolean,
    isEmpty: Boolean,
    exercises: ImmutableList<LiveExerciseUi>,
    unit: UnitSystem,
    editingField: SetFieldRef?,
    keypadTarget: LiveKeypadTargetUi?,
    weightStep: WeightStep,
    snackbarHostState: RepzSnackbarHostState,
    onNameClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onNameEditingFinish: () -> Unit,
    onMinimizeClick: () -> Unit,
    onFinishClick: () -> Unit,
    onDiscardClick: () -> Unit,
    onAddExerciseClick: () -> Unit,
    onReorderClick: () -> Unit,
    onNotesClick: (WorkoutExerciseId) -> Unit,
    onNotesChange: (WorkoutExerciseId, String) -> Unit,
    onNotesEditingFinish: (WorkoutExerciseId) -> Unit,
    onRemoveClick: (WorkoutExerciseId) -> Unit,
    onAddSetClick: (WorkoutExerciseId) -> Unit,
    onSetFieldClick: (WorkoutSetId, SetField) -> Unit,
    onSetCompleteClick: (WorkoutSetId) -> Unit,
    onSetDeleteClick: (WorkoutSetId) -> Unit,
    onSetTypeSelect: (WorkoutSetId, SetType) -> Unit,
    onWeightEnter: (WorkoutSetId, Weight?) -> Unit,
    onRepsEnter: (WorkoutSetId, Int?) -> Unit,
    onRpeEnter: (WorkoutSetId, Rpe?) -> Unit,
    onKeypadFieldSelect: (SetField) -> Unit,
    onWeightStepChange: (WeightStep) -> Unit,
    onNextField: () -> Unit,
    onKeypadDismiss: () -> Unit,
) {
    val listState = rememberLazyListState()
    val layoutDirection = LocalLayoutDirection.current
    var keypadHeightPx by remember { mutableIntStateOf(0) }
    val isKeypadOpen = keypadTarget != null

    LiveWorkoutKeypadFocusEffect(isKeypadOpen)
    LiveWorkoutAutoScrollEffect(targetSetId = keypadTarget?.setId, exercises = exercises, listState = listState)

    Box(modifier = Modifier.fillMaxSize().dismissKeypadOnOutsideTap(isKeypadOpen, onKeypadDismiss)) {
        LiveWorkoutScaffoldSection(
            workoutName = workoutName,
            elapsedText = formatElapsed(elapsed),
            isFinishEnabled = isFinishEnabled,
            isEditingName = isEditingName,
            canAddExercise = canAddExercise,
            isKeypadOpen = isKeypadOpen,
            isEmpty = isEmpty,
            exercises = exercises,
            unit = unit,
            editingField = editingField,
            snackbarHostState = snackbarHostState,
            listState = listState,
            keypadHeightPx = keypadHeightPx,
            layoutDirection = layoutDirection,
            onNameClick = onNameClick,
            onNameChange = onNameChange,
            onNameEditingFinish = onNameEditingFinish,
            onMinimizeClick = onMinimizeClick,
            onFinishClick = onFinishClick,
            onDiscardClick = onDiscardClick,
            onAddExerciseClick = onAddExerciseClick,
            onReorderClick = onReorderClick,
            onNotesClick = onNotesClick,
            onNotesChange = onNotesChange,
            onNotesEditingFinish = onNotesEditingFinish,
            onRemoveClick = onRemoveClick,
            onAddSetClick = onAddSetClick,
            onSetFieldClick = onSetFieldClick,
            onSetCompleteClick = onSetCompleteClick,
            onSetDeleteClick = onSetDeleteClick,
            onSetTypeSelect = onSetTypeSelect,
        )

        LiveWorkoutKeypad(
            keypadTarget = keypadTarget,
            unit = unit,
            weightStep = weightStep,
            onWeightEnter = onWeightEnter,
            onRepsEnter = onRepsEnter,
            onRpeEnter = onRpeEnter,
            onFieldSelect = onKeypadFieldSelect,
            onWeightStepChange = onWeightStepChange,
            onNext = onNextField,
            onDismiss = onKeypadDismiss,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .consumeUnhandledTapsInsideSheet()
                    .onSizeChanged { size -> keypadHeightPx = size.height },
        )
    }
}

/**
 * Dismisses by calling [onDismiss] on a tap that nothing else consumes (board 09 frame 11: the
 * keypad sheet is non-modal, so a field, button or scroll drag underneath wins the gesture first).
 * Attached to an ancestor of both the content and the keypad; [consumeUnhandledTapsInsideSheet] on
 * the keypad itself consumes a blank-area tap there first, so only a tap truly outside it arrives
 * here still unconsumed.
 */
private fun Modifier.dismissOnUnconsumedTap(onDismiss: () -> Unit): Modifier =
    pointerInput(onDismiss) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            if (waitForUpOrCancellation() != null) onDismiss()
        }
    }

/**
 * Consumes any tap that reaches the keypad sheet itself unhandled — a gap between keys, the
 * display, the drag handle — before it can bubble up to [dismissOnUnconsumedTap]'s ancestor; only a
 * tap that lands outside the sheet's own bounds should dismiss it.
 */
private fun Modifier.consumeUnhandledTapsInsideSheet(): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            waitForUpOrCancellation()?.consume()
        }
    }

/** Only attaches [dismissOnUnconsumedTap] while [isKeypadOpen]; a closed keypad needs no dismiss gesture. */
private fun Modifier.dismissKeypadOnOutsideTap(
    isKeypadOpen: Boolean,
    onDismiss: () -> Unit,
): Modifier = if (isKeypadOpen) dismissOnUnconsumedTap(onDismiss) else this

/** The coarse half of the row-above-keypad scroll (the other half is
 * [LiveWorkoutSetRow][com.repz.app.feature.workout.impl.presentation.live.LiveWorkoutExerciseCard]'s
 * own `BringIntoViewRequester`): a card the user hasn't scrolled to yet isn't composed at all, so a
 * `BringIntoViewRequester` attached to one of its rows can't act on it yet. This jumps there once,
 * instantly, only when the card isn't already visible; the row's own effect then does the one
 * animated, keypad-aware positioning the user actually sees, so the two never fight over the scroll. */
@Composable
private fun LiveWorkoutAutoScrollEffect(
    targetSetId: WorkoutSetId?,
    exercises: ImmutableList<LiveExerciseUi>,
    listState: LazyListState,
) {
    LaunchedEffect(targetSetId) {
        val setId = targetSetId ?: return@LaunchedEffect
        val exerciseIndex = exercises.indexOfFirst { exercise -> exercise.sets.any { it.id == setId } }
        val isAlreadyVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == exerciseIndex }
        if (exerciseIndex >= 0 && !isAlreadyVisible) listState.scrollToItem(exerciseIndex)
    }
}

/** The header, bottom bar, snackbar host and exercise list inside [RepzLiveWorkoutScaffold]. */
@Composable
private fun LiveWorkoutScaffoldSection(
    workoutName: String,
    elapsedText: String,
    isFinishEnabled: Boolean,
    isEditingName: Boolean,
    canAddExercise: Boolean,
    isKeypadOpen: Boolean,
    isEmpty: Boolean,
    exercises: ImmutableList<LiveExerciseUi>,
    unit: UnitSystem,
    editingField: SetFieldRef?,
    snackbarHostState: RepzSnackbarHostState,
    listState: LazyListState,
    keypadHeightPx: Int,
    layoutDirection: LayoutDirection,
    onNameClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onNameEditingFinish: () -> Unit,
    onMinimizeClick: () -> Unit,
    onFinishClick: () -> Unit,
    onDiscardClick: () -> Unit,
    onAddExerciseClick: () -> Unit,
    onReorderClick: () -> Unit,
    onNotesClick: (WorkoutExerciseId) -> Unit,
    onNotesChange: (WorkoutExerciseId, String) -> Unit,
    onNotesEditingFinish: (WorkoutExerciseId) -> Unit,
    onRemoveClick: (WorkoutExerciseId) -> Unit,
    onAddSetClick: (WorkoutExerciseId) -> Unit,
    onSetFieldClick: (WorkoutSetId, SetField) -> Unit,
    onSetCompleteClick: (WorkoutSetId) -> Unit,
    onSetDeleteClick: (WorkoutSetId) -> Unit,
    onSetTypeSelect: (WorkoutSetId, SetType) -> Unit,
) {
    val keypadHeightDp = with(LocalDensity.current) { keypadHeightPx.toDp() }
    RepzLiveWorkoutScaffold(
        header = {
            LiveWorkoutHeaderSection(
                name = workoutName,
                elapsedText = elapsedText,
                isFinishEnabled = isFinishEnabled,
                isEditingName = isEditingName,
                onNameClick = onNameClick,
                onNameChange = onNameChange,
                onNameEditingFinish = onNameEditingFinish,
                onMinimizeClick = onMinimizeClick,
                onFinishClick = onFinishClick,
                onDiscardClick = onDiscardClick,
            )
        },
        bottomBar = {
            if (!isKeypadOpen) {
                LiveWorkoutBottomBar(canAddExercise = canAddExercise, onAddExerciseClick = onAddExerciseClick)
            }
        },
        snackbarHost = { RepzSnackbarHost(state = snackbarHostState) },
    ) { innerPadding ->
        LiveWorkoutExerciseList(
            isEmpty = isEmpty,
            exercises = exercises,
            unit = unit,
            editingField = editingField,
            keypadHeightPx = keypadHeightPx,
            onAddExercisesClick = onAddExerciseClick,
            onReorderClick = onReorderClick,
            onNotesClick = onNotesClick,
            onNotesChange = onNotesChange,
            onNotesEditingFinish = onNotesEditingFinish,
            onRemoveClick = onRemoveClick,
            onAddSetClick = onAddSetClick,
            onSetFieldClick = onSetFieldClick,
            onSetCompleteClick = onSetCompleteClick,
            onSetDeleteClick = onSetDeleteClick,
            onSetTypeSelect = onSetTypeSelect,
            listState = listState,
            contentPadding = innerPadding.withExtraBottom(keypadHeightDp, layoutDirection),
        )
    }
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
    RepzLiveWorkoutBottomBar(
        addExerciseLabel = stringResource(Res.string.live_add_exercise),
        isAddExerciseEnabled = canAddExercise,
        onAddExerciseClick = onAddExerciseClick,
        addExerciseButtonModifier = Modifier.testTag("live_add_exercise"),
    )
}

/** Maps [keypadTarget] to [SetValueKeypad]'s own request; `null` hides the sheet. Every callback
 * here takes the affected [WorkoutSetId] explicitly instead of the set's own intent. */
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
