package com.repz.app.feature.workout.impl.presentation.live

import androidx.compose.runtime.Composable
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

/**
 * System Back and predictive back: dismisses the keypad while it's open, otherwise exits reorder
 * mode, otherwise minimizes (board 09's only other Back target). [isKeypadOpen]/[isReordering] are
 * mutually exclusive, so the three handlers never compete; each needs its own hoisted state per
 * `NavigationEventHandler`'s contract.
 */
@Composable
internal fun LiveWorkoutBackHandling(
    isKeypadOpen: Boolean,
    isReordering: Boolean,
    onKeypadDismiss: () -> Unit,
    onReorderExit: () -> Unit,
    onMinimize: () -> Unit,
) {
    val keypadDismissState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(state = keypadDismissState, isBackEnabled = isKeypadOpen, onBackCompleted = onKeypadDismiss)

    val reorderExitState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(state = reorderExitState, isBackEnabled = !isKeypadOpen && isReordering, onBackCompleted = onReorderExit)

    val minimizeState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(state = minimizeState, isBackEnabled = !isKeypadOpen && !isReordering, onBackCompleted = onMinimize)
}
