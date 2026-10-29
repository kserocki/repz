package com.repz.app.core.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * Keeps one back stack per [TopLevelDestination] plus a separate back stack for the signed-out
 * auth flow, and the single source of truth for which of them is on screen.
 *
 * Rules: switching tabs keeps every tab's stack; re-selecting the current tab pops it to its
 * root; `goBack()` at the root of a non-[TopLevelDestination.Train] tab selects Train with its
 * stack untouched, and at Train's root returns `false` (the caller, e.g. the platform back
 * button, decides what happens next); navigating to the key already on top of the active stack
 * is a no-op; signing in resets every tab to its root and selects Train; signing out resets
 * every tab to its root and restarts the auth stack at its root.
 *
 * `isAuthenticated` and `selectedDestination` are snapshot-backed ([mutableStateOf]) and the
 * back stacks are [NavBackStack]s (snapshot-backed lists), so a Compose caller observes every
 * mutation without extra plumbing. There is no [androidx.compose.runtime.Composable] here: this
 * class is a plain, UI-framework-agnostic state holder.
 */
class Navigator(
    private val topLevelRoots: Map<TopLevelDestination, NavKey>,
    private val authRoot: NavKey,
    isAuthenticated: Boolean,
    restoredState: NavigatorState? = null,
) {
    init {
        val missingRoots = TopLevelDestination.entries - topLevelRoots.keys
        require(missingRoots.isEmpty()) {
            "topLevelRoots is missing a root for: $missingRoots"
        }
    }

    private val backStacks: Map<TopLevelDestination, NavBackStack<NavKey>> =
        TopLevelDestination.entries.associateWith { destination ->
            val restoredEntries = restoredState?.topLevelBackStacks?.get(destination)
            NavBackStack<NavKey>().apply { addAll(entriesOrRoot(restoredEntries, topLevelRoots.getValue(destination))) }
        }

    private val authBackStack: NavBackStack<NavKey> =
        NavBackStack<NavKey>().apply { addAll(entriesOrRoot(restoredState?.authBackStack, authRoot)) }

    var isAuthenticated: Boolean by mutableStateOf(restoredState?.isAuthenticated ?: isAuthenticated)
        private set

    var selectedDestination: TopLevelDestination by mutableStateOf(restoredState?.selectedDestination ?: TopLevelDestination.Train)
        private set

    val visibleBackStack: List<NavKey>
        get() = when {
            !isAuthenticated -> authBackStack.toList()
            selectedDestination == TopLevelDestination.Train -> backStacks.getValue(TopLevelDestination.Train).toList()
            else -> backStacks.getValue(TopLevelDestination.Train).toList() + backStacks.getValue(selectedDestination).toList()
        }

    /** Pushes [key] on the active stack (auth stack signed out, selected tab signed in); ignored if [key] is already on top. */
    fun navigate(key: NavKey) {
        val activeStack = if (isAuthenticated) backStacks.getValue(selectedDestination) else authBackStack
        if (activeStack.lastOrNull() != key) {
            activeStack.add(key)
        }
    }

    /** Switches to [destination], keeping its stack; selecting the already-active tab pops it to its root. */
    fun selectTopLevel(destination: TopLevelDestination) {
        if (destination == selectedDestination) {
            resetToRoot(destination)
        } else {
            selectedDestination = destination
        }
    }

    /**
     * Pops one entry off the active stack. Returns `false` when there is nothing to pop (the
     * auth stack's root signed out, or Train's root signed in), leaving it to the caller.
     */
    fun goBack(): Boolean {
        if (!isAuthenticated) {
            return popOrFalse(authBackStack)
        }
        if (selectedDestination != TopLevelDestination.Train) {
            val selectedStack = backStacks.getValue(selectedDestination)
            return if (selectedStack.size > 1) {
                selectedStack.removeAt(selectedStack.lastIndex)
                true
            } else {
                selectedDestination = TopLevelDestination.Train
                true
            }
        }
        return popOrFalse(backStacks.getValue(TopLevelDestination.Train))
    }

    /** Resets every tab to its root and selects [TopLevelDestination.Train]. */
    fun onSignedIn() {
        TopLevelDestination.entries.forEach(::resetToRoot)
        selectedDestination = TopLevelDestination.Train
        isAuthenticated = true
    }

    /** Resets every tab to its root and restarts the auth stack at its root. */
    fun onSignedOut() {
        TopLevelDestination.entries.forEach(::resetToRoot)
        authBackStack.clear()
        authBackStack.add(authRoot)
        isAuthenticated = false
    }

    fun saveState(): NavigatorState = NavigatorState(
        isAuthenticated = isAuthenticated,
        selectedDestination = selectedDestination,
        topLevelBackStacks = backStacks.mapValues { (_, stack) -> stack.toList() },
        authBackStack = authBackStack.toList(),
    )

    private fun resetToRoot(destination: TopLevelDestination) {
        val stack = backStacks.getValue(destination)
        stack.clear()
        stack.add(topLevelRoots.getValue(destination))
    }

    private fun popOrFalse(stack: NavBackStack<NavKey>): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    private fun entriesOrRoot(restored: List<NavKey>?, root: NavKey): List<NavKey> =
        restored.takeUnless { it.isNullOrEmpty() } ?: listOf(root)
}
