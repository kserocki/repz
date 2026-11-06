package com.repz.app.feature.workout.impl.presentation.live

import androidx.compose.runtime.Composable
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

/**
 * System Back and predictive back (keypad plan §8.2): dismisses the keypad while it's open,
 * otherwise exits reorder mode while reordering, otherwise minimizes (board 09's only other Back
 * target). Exactly one of [isKeypadOpen]/[isReordering] is true at a time, so precedence between
 * the three handlers never matters; each needs its own hoisted
 * [androidx.navigationevent.NavigationEventState] (one handler per state, per
 * `NavigationEventHandler`'s own contract). Only `*Screen`/`*Content` may know [LiveWorkoutIntent]
 * (AGENTS.md §4, Konsist N8), so this takes plain flags and present-tense callbacks.
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
