package com.repz.app.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Wraps M3's [SnackbarHostState] so a `*ViewModel`'s effect handler has one call for "show a message
 * with an action and tell me whether it was tapped" (e.g. `ShowUnexpectedError` → Retry), instead of
 * every caller re-deriving a [SnackbarResult].
 */
@Stable
class RepzSnackbarHostState internal constructor(
    internal val delegate: SnackbarHostState,
) {
    /** Shows [message] with an [actionLabel] action; returns `true` only if that action was tapped.
     * [SnackbarDuration.Indefinite] is passed explicitly (M3's own default for an action-bearing
     * snackbar already resolves to this, review-1) so a caller wrapping this in its own timeout
     * (e.g. an undo window) is the only thing that can end it early, regardless of M3's own default. */
    suspend fun showWithAction(
        message: String,
        actionLabel: String,
    ): Boolean {
        val result = delegate.showSnackbar(message = message, actionLabel = actionLabel, duration = SnackbarDuration.Indefinite)
        return result == SnackbarResult.ActionPerformed
    }

    /** Shows a plain, action-less [message] (M3's own default duration and dismiss behaviour
     * apply); for a generic failure notice that has no follow-up action, unlike [showWithAction]. */
    suspend fun show(message: String) {
        delegate.showSnackbar(message = message)
    }

    /** Dismisses the currently shown snackbar, if any, without waiting for its
     * [showWithAction] call to return. Callers use this so a stale message (e.g. a previous
     * error) never survives a fresh resubmit. */
    fun dismissCurrent() {
        delegate.currentSnackbarData?.dismiss()
    }
}

@Composable
fun rememberRepzSnackbarHostState(): RepzSnackbarHostState {
    val delegate = remember { SnackbarHostState() }
    return remember(delegate) { RepzSnackbarHostState(delegate) }
}

/** M3's default snackbar look (`inverseSurface`/`inverseOnSurface`), `shapes.small`; further Repz
 * styling (e.g. an icon slot) lands later. */
@Composable
fun RepzSnackbarHost(
    state: RepzSnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = state.delegate, modifier = modifier) { data ->
        Snackbar(snackbarData = data, shape = MaterialTheme.shapes.small)
    }
}
