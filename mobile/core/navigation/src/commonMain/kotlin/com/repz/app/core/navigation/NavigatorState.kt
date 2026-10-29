package com.repz.app.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** A full, saveable snapshot of a [Navigator]. */
@Serializable
data class NavigatorState(
    val isAuthenticated: Boolean,
    val selectedDestination: TopLevelDestination,
    val topLevelBackStacks: Map<TopLevelDestination, List<NavKey>>,
    val authBackStack: List<NavKey>,
)
