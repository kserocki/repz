package com.repz.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation3.runtime.NavKey
import androidx.savedstate.SavedState
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState

/**
 * Remembers a [Navigator] across recompositions and saves/restores it through [SavedState]
 * (`Bundle` on Android, a plain class on iOS): the same `androidx.savedstate.serialization` path
 * `rememberSerializable` uses, applied here to [Navigator.saveState]'s snapshot since [Navigator]
 * itself is a stateful class, not a plain serializable value.
 *
 * Call this unconditionally at the top of the app shell, never inside a branch gated by the auth
 * gate's loading state, so the saveable key stays stable across recompositions.
 */
@Composable
fun rememberNavigator(
    topLevelRoots: Map<TopLevelDestination, NavKey>,
    authRoot: NavKey,
    configuration: SavedStateConfiguration,
): Navigator =
    rememberSaveable(saver = navigatorSaver(topLevelRoots, authRoot, configuration)) {
        Navigator(topLevelRoots = topLevelRoots, authRoot = authRoot, isAuthenticated = false)
    }

private fun navigatorSaver(
    topLevelRoots: Map<TopLevelDestination, NavKey>,
    authRoot: NavKey,
    configuration: SavedStateConfiguration,
): Saver<Navigator, SavedState> =
    Saver(
        save = { navigator -> encodeToSavedState(NavigatorState.serializer(), navigator.saveState(), configuration) },
        restore = { savedState ->
            Navigator(
                topLevelRoots = topLevelRoots,
                authRoot = authRoot,
                isAuthenticated = false,
                restoredState = decodeFromSavedState(NavigatorState.serializer(), savedState, configuration),
            )
        },
    )
