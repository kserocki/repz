package com.repz.app.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
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
    /** Shows [message] with an [actionLabel] action; returns `true` only if that action was tapped
     * (M3's own default duration and dismiss behaviour apply). */
    suspend fun showWithAction(
        message: String,
        actionLabel: String,
    ): Boolean {
        val result = delegate.showSnackbar(message = message, actionLabel = actionLabel)
        return result == SnackbarResult.ActionPerformed
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
