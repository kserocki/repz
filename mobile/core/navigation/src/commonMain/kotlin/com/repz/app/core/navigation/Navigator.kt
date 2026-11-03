package com.repz.app.core.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * Keeps one back stack per [TopLevelDestination] plus a back stack for the signed-out auth flow.
 * Switching tabs keeps every stack; re-selecting the current tab pops it to its root; `goBack()`
 * at a non-Train root selects Train, at Train's root or the auth root returns `false`; pushing
 * the key already on top is a no-op; sign-in resets every tab and selects Train; sign-out resets
 * every tab and restarts the auth stack; [selectTopLevel] is ignored while signed out;
 * [syncAuthentication] is the only way a caller should move between the two.
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
        get() = visibleEntries.map { it.key }

    /** [visibleBackStack], with each key tagged by the stack that owns it and its depth there. */
    val visibleEntries: List<VisibleNavKey>
        get() =
            when {
                !isAuthenticated -> {
                    taggedEntries(authBackStack, BackStackId.Auth)
                }

                selectedDestination == TopLevelDestination.Train -> {
                    taggedEntries(backStacks.getValue(TopLevelDestination.Train), BackStackId.Tab(TopLevelDestination.Train))
                }

                else -> {
                    taggedEntries(backStacks.getValue(TopLevelDestination.Train), BackStackId.Tab(TopLevelDestination.Train)) +
                        taggedEntries(backStacks.getValue(selectedDestination), BackStackId.Tab(selectedDestination))
                }
            }

    /** Pushes [key] on the active stack (auth stack signed out, selected tab signed in); ignored if [key] is already on top. */
    fun navigate(key: NavKey) {
        val activeStack = if (isAuthenticated) backStacks.getValue(selectedDestination) else authBackStack
        if (activeStack.lastOrNull() != key) {
            activeStack.add(key)
        }
    }

    /** Switches to [destination], keeping its stack; selecting the already-active tab pops it to its root. Ignored while signed out. */
    fun selectTopLevel(destination: TopLevelDestination) {
        if (!isAuthenticated) return
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
    fun goBack(): Boolean =
        when {
            !isAuthenticated -> {
                popOrFalse(authBackStack)
            }

            selectedDestination != TopLevelDestination.Train -> {
                val selectedStack = backStacks.getValue(selectedDestination)
                if (selectedStack.size > 1) {
                    selectedStack.removeAt(selectedStack.lastIndex)
                } else {
                    selectedDestination = TopLevelDestination.Train
                }
                true
            }

            else -> {
                popOrFalse(backStacks.getValue(TopLevelDestination.Train))
            }
        }

    /** Brings the navigator in line with the real session; a no-op when it already matches. */
    fun syncAuthentication(isSignedIn: Boolean) {
        if (isSignedIn == isAuthenticated) return
        if (isSignedIn) onSignedIn() else onSignedOut()
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

    fun saveState(): NavigatorState =
        NavigatorState(
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

    private fun entriesOrRoot(
        restored: List<NavKey>?,
        root: NavKey,
    ): List<NavKey> = restored.takeUnless { it.isNullOrEmpty() } ?: listOf(root)

    private fun taggedEntries(
        stack: NavBackStack<NavKey>,
        stackId: BackStackId,
    ): List<VisibleNavKey> = stack.mapIndexed { depth, key -> VisibleNavKey(key, stackId, depth) }
}
