package com.repz.app.feature.workout.impl.presentation.live

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/** Clears whatever notes/rename field has focus and hides the IME: called on Minimize, Finish and
 * keypad-open, none of which leaves room for a focused field's keyboard, and keeps the live
 * workout's header reachable on iOS instead of the OS panning the whole screen up. */
@Composable
internal fun rememberEndFieldEditing(): () -> Unit {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    return remember(focusManager, keyboardController) {
        {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }
}

/** [action], preceded by ending whatever notes/rename field edit is in progress: for a header
 * button that leaves the screen, so it never leaves a field's keyboard open behind it. */
@Composable
internal fun rememberEndingFieldEditing(action: () -> Unit): () -> Unit {
    val endFieldEditing = rememberEndFieldEditing()
    return remember(endFieldEditing, action) {
        {
            endFieldEditing()
            action()
        }
    }
}

/** Ends field editing the moment [isKeypadOpen] becomes `true`, so the keypad never opens behind
 * a still-focused notes/rename field and its keyboard. */
@Composable
internal fun LiveWorkoutKeypadFocusEffect(isKeypadOpen: Boolean) {
    val endFieldEditing = rememberEndFieldEditing()
    LaunchedEffect(isKeypadOpen) { if (isKeypadOpen) endFieldEditing() }
}
