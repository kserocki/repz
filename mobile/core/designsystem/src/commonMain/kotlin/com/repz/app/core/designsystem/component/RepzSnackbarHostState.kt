package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// T4: the undo snackbar's base timeout before the platform's own accessibility-recommended
// extension (TalkBack/VoiceOver get longer than this).
private const val UNDO_SNACKBAR_BASE_TIMEOUT_MILLIS = 6_000L

// Board 06: the snackbar's own minimum height.
private val SnackbarMinHeight = 48.dp

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

    /**
     * Board 06/DESIGN_SYSTEM §9: shows [message] with an [undoLabel] action. M3 only offers 4 s/
     * 10 s/indefinite durations, none of which is the 6 s DESIGN_SYSTEM §9 specifies (T4), so this
     * shows with [RepzUndoSnackbarVisuals] ([SnackbarDuration.Indefinite]) and lets
     * [RepzSnackbarHost] itself auto-dismiss it after the accessibility-adjusted timeout
     * ([undoSnackbarTimeoutMillis]). Returns `true` only if [undoLabel] was tapped before that.
     *
     * [showWithAction] also shows `Indefinite` whenever M3's own default applies (any action label),
     * so [RepzSnackbarHost] tells the two apart with [autoDismissesAfterUndoTimeout], not by
     * [SnackbarDuration] — otherwise every `showWithAction` caller (e.g. the auth screens' Retry
     * snackbar) would also auto-dismiss after 6 s, which M3's own behaviour never does.
     */
    suspend fun showUndo(
        message: String,
        undoLabel: String,
    ): Boolean {
        val result = delegate.showSnackbar(RepzUndoSnackbarVisuals(message = message, actionLabel = undoLabel))
        return result == SnackbarResult.ActionPerformed
    }

    /** A plain message with no action, M3's own default ([SnackbarDuration.Short]) duration. */
    suspend fun showMessage(message: String) {
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

/**
 * T4: [UNDO_SNACKBAR_BASE_TIMEOUT_MILLIS] before [recommendedTimeoutMillis] (normally
 * `AccessibilityManager.calculateRecommendedTimeoutMillis`) extends it for TalkBack/VoiceOver users.
 * A plain function (not `@Composable`), so a unit test can fake [recommendedTimeoutMillis] without a
 * live `AccessibilityManager`.
 */
internal fun undoSnackbarTimeoutMillis(recommendedTimeoutMillis: (Long) -> Long): Long =
    recommendedTimeoutMillis(UNDO_SNACKBAR_BASE_TIMEOUT_MILLIS)

/**
 * Marks a snackbar as [RepzSnackbarHostState.showUndo]'s own kind, so [RepzSnackbarHost] knows which
 * [SnackbarDuration.Indefinite] snackbars to auto-dismiss on a timeout. Duration alone can't tell the
 * two apart: M3's own `showSnackbar(message, actionLabel)` (the path [RepzSnackbarHostState
 * .showWithAction] uses) already defaults to `Indefinite` whenever an action label is set, without
 * [RepzSnackbarHost] auto-dismissing it.
 */
internal class RepzUndoSnackbarVisuals(
    override val message: String,
    override val actionLabel: String,
) : SnackbarVisuals {
    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration = SnackbarDuration.Indefinite
}

/** True only for a snackbar shown by [RepzSnackbarHostState.showUndo] — the one kind
 * [RepzSnackbarHost] auto-dismisses on a timeout. */
internal fun SnackbarVisuals.autoDismissesAfterUndoTimeout(): Boolean = this is RepzUndoSnackbarVisuals

/**
 * Board 06: 48 dp min height, `shapes.small`, M3's own `inverseSurface`/`inverseOnSurface`, the
 * action in `inversePrimary` at `labelLargeEmphasized` (M3's own action style defaults to the
 * unemphasized `labelLarge`). Only a [RepzUndoSnackbarVisuals] snackbar (shown by
 * [RepzSnackbarHostState.showUndo]) auto-dismisses itself after [undoSnackbarTimeoutMillis]'s
 * accessibility-adjusted timeout; every other snackbar (including an `Indefinite`
 * [RepzSnackbarHostState.showWithAction] one) keeps M3's own behaviour, since M3 never times one out
 * on its own.
 */
@Composable
fun RepzSnackbarHost(
    state: RepzSnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = state.delegate, modifier = modifier) { data ->
        if (data.visuals.autoDismissesAfterUndoTimeout()) {
            val accessibilityManager = LocalAccessibilityManager.current
            LaunchedEffect(data) {
                val timeoutMillis =
                    undoSnackbarTimeoutMillis { baseTimeoutMillis ->
                        accessibilityManager?.calculateRecommendedTimeoutMillis(
                            baseTimeoutMillis,
                            containsIcons = false,
                            containsText = true,
                            containsControls = true,
                        ) ?: baseTimeoutMillis
                    }
                delay(timeoutMillis)
                data.dismiss()
            }
        }

        val actionLabel = data.visuals.actionLabel
        Snackbar(
            modifier = Modifier.heightIn(min = SnackbarMinHeight),
            action =
                actionLabel?.let { label ->
                    {
                        TextButton(
                            onClick = { data.performAction() },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.inversePrimary),
                        ) {
                            Text(label, style = MaterialTheme.typography.labelLargeEmphasized)
                        }
                    }
                },
            shape = MaterialTheme.shapes.small,
        ) {
            Text(data.visuals.message)
        }
    }
}
