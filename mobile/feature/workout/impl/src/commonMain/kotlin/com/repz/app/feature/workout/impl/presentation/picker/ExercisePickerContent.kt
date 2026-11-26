package com.repz.app.feature.workout.impl.presentation.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.IntSize
import com.repz.app.core.designsystem.component.RepzSnackbarHost
import com.repz.app.core.designsystem.component.RepzSnackbarHostState
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.domain.model.Equipment
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.Muscle

/**
 * The exercise picker (DESIGN_SYSTEM §9, board 10 C13): a plain full-screen `Column` — not an M3
 * `Scaffold` — because the bottom bar must float over the list (like the live workout's keypad) and
 * sit above an open keyboard, with its own measured height feeding the list's bottom content padding.
 * [ExercisePickerIntent] is this function's alone (ADR-0005/Konsist N4); every composable it calls
 * takes explicit values and present-tense callbacks.
 */
@Composable
fun ExercisePickerContent(
    state: ExercisePickerUiState,
    onIntent: (ExercisePickerIntent) -> Unit,
    snackbarHostState: RepzSnackbarHostState,
    modifier: Modifier = Modifier,
) {
    var queryText by remember { mutableStateOf(state.query) }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var bottomBarHeightPx by remember { mutableIntStateOf(0) }
    val showBottomBar = state.mode == ExercisePickerMode.Add && state.selectedCount > 0

    val endFieldEditing: () -> Unit =
        remember(focusManager, keyboardController) {
            {
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
            }
        }

    // Every keystroke/filter toggle jumps the list to the top; a drag-scroll hides the keyboard.
    LaunchedEffect(state.query, state.activeFilterCount) { listState.scrollToItem(0) }
    LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) endFieldEditing() }

    val onExerciseClick: (ExerciseId) -> Unit = { id ->
        if (state.mode == ExercisePickerMode.Replace) endFieldEditing()
        onIntent(ExercisePickerIntent.ExerciseClicked(id))
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        ExercisePickerScaffoldColumn(
            state = state,
            queryText = queryText,
            onQueryChange = { newQuery ->
                queryText = newQuery
                onIntent(
                    if (newQuery.isEmpty()) ExercisePickerIntent.QueryCleared else ExercisePickerIntent.QueryChanged(newQuery),
                )
            },
            onSearch = endFieldEditing,
            onCloseClick = {
                endFieldEditing()
                onIntent(ExercisePickerIntent.CloseClicked)
            },
            onMuscleToggle = { muscle -> onIntent(ExercisePickerIntent.MuscleFilterToggled(muscle)) },
            onEquipmentToggle = { equipment -> onIntent(ExercisePickerIntent.EquipmentFilterToggled(equipment)) },
            onClearFiltersClick = { onIntent(ExercisePickerIntent.FiltersCleared) },
            onExerciseClick = onExerciseClick,
            listState = listState,
            showBottomBar = showBottomBar,
            bottomBarHeightPx = bottomBarHeightPx,
        )

        ExercisePickerOverlays(
            snackbarHostState = snackbarHostState,
            showBottomBar = showBottomBar,
            selectedCount = state.selectedCount,
            isSubmitting = state.isSubmitting,
            replaceConfirmation = state.replaceConfirmation,
            onAddClick = {
                endFieldEditing()
                onIntent(ExercisePickerIntent.AddClicked)
            },
            onBottomBarSizeChange = { size -> bottomBarHeightPx = size.height },
            onReplaceConfirm = { onIntent(ExercisePickerIntent.ReplaceConfirmed) },
            onReplaceDismiss = { onIntent(ExercisePickerIntent.ReplaceDismissed) },
        )
    }
}

/** The top section and the list/empty-state body, stacked in one [Column] (the search bar, chip row
 * and list all scroll/measure together, independent of the floating bottom bar and snackbar). */
@Composable
private fun ExercisePickerScaffoldColumn(
    state: ExercisePickerUiState,
    queryText: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onCloseClick: () -> Unit,
    onMuscleToggle: (Muscle) -> Unit,
    onEquipmentToggle: (Equipment) -> Unit,
    onClearFiltersClick: () -> Unit,
    onExerciseClick: (ExerciseId) -> Unit,
    listState: LazyListState,
    showBottomBar: Boolean,
    bottomBarHeightPx: Int,
) {
    val density = LocalDensity.current
    val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(modifier = Modifier.fillMaxSize()) {
        ExercisePickerTopSection(
            query = queryText,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            onCloseClick = onCloseClick,
            muscleChips = state.muscleChips,
            equipmentChips = state.equipmentChips,
            onMuscleToggle = onMuscleToggle,
            onEquipmentToggle = onEquipmentToggle,
        )

        Box(modifier = Modifier.weight(1f)) {
            if (state.emptyState != null) {
                ExercisePickerEmptyStateSection(emptyState = state.emptyState, onClearFiltersClick = onClearFiltersClick)
            } else {
                ExercisePickerList(
                    state = state,
                    onExerciseClick = onExerciseClick,
                    listState = listState,
                    contentPadding =
                        PaddingValues(
                            start = RepzSpacing.sm,
                            end = RepzSpacing.sm,
                            bottom = if (showBottomBar) with(density) { bottomBarHeightPx.toDp() } else navigationBarInset,
                        ),
                )
            }
        }
    }
}

/** The floating snackbar host and Add-mode bottom bar (bottom-aligned, snackbar above the bar), plus
 * the Replace confirmation dialog — all layered over [ExercisePickerScaffoldColumn] in the parent [Box]. */
@Composable
private fun BoxScope.ExercisePickerOverlays(
    snackbarHostState: RepzSnackbarHostState,
    showBottomBar: Boolean,
    selectedCount: Int,
    isSubmitting: Boolean,
    replaceConfirmation: ReplaceConfirmationUi?,
    onAddClick: () -> Unit,
    onBottomBarSizeChange: (IntSize) -> Unit,
    onReplaceConfirm: () -> Unit,
    onReplaceDismiss: () -> Unit,
) {
    Column(modifier = Modifier.align(Alignment.BottomCenter)) {
        RepzSnackbarHost(state = snackbarHostState)
        if (showBottomBar) {
            ExercisePickerBottomBar(
                selectedCount = selectedCount,
                isSubmitting = isSubmitting,
                onAddClick = onAddClick,
                modifier = Modifier.onSizeChanged(onBottomBarSizeChange),
            )
        }
    }

    replaceConfirmation?.let { confirmation ->
        ExercisePickerReplaceDialog(confirmation = confirmation, onConfirm = onReplaceConfirm, onDismiss = onReplaceDismiss)
    }
}
