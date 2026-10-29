package com.repz.app.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * A full snapshot of a [Navigator]: every tab's back stack, the auth back stack, the selected
 * tab and whether the user is signed in. [NavKey] is an interface, so its occurrences here are
 * serialized polymorphically through the [kotlinx.serialization.modules.SerializersModule]
 * passed to the `Json` or [androidx.savedstate.serialization.SavedStateConfiguration] used to
 * (de)serialize this type.
 */
@Serializable
data class NavigatorState(
    val isAuthenticated: Boolean,
    val selectedDestination: TopLevelDestination,
    val topLevelBackStacks: Map<TopLevelDestination, List<NavKey>>,
    val authBackStack: List<NavKey>,
)
