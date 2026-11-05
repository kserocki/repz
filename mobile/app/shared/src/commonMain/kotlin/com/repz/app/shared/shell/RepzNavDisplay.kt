package com.repz.app.shared.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.repz.app.core.navigation.BackStackId
import com.repz.app.core.navigation.Navigator
import com.repz.app.core.navigation.TopLevelDestination
import com.repz.app.core.navigation.VisibleNavKey

/**
 * Hosts [navigator] through [repzEntries]: every one of [navigator]'s back stacks (the 4 tabs plus
 * auth) is decorated unconditionally by [rememberStackEntries], each with its own saveable-state
 * and `ViewModelStore` decorators, so a stack keeps its screens' state and ViewModels while it
 * isn't on screen; only [Navigator.visibleStacks]' decorated entries are then handed to
 * [NavDisplay] (ADR-0025).
 */
@Composable
internal fun RepzNavDisplay(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val provider = remember(navigator) { stackScoped(entryProvider { repzEntries(navigator) }) }

    val sessionEpoch = navigator.sessionEpoch

    val entriesByStack: Map<BackStackId, List<NavEntry<VisibleNavKey>>> =
        mapOf(
            BackStackId.Auth to rememberStackEntries(BackStackId.Auth, navigator.authBackStack, sessionEpoch, provider),
            BackStackId.Tab(TopLevelDestination.Train) to
                rememberStackEntries(
                    BackStackId.Tab(TopLevelDestination.Train),
                    navigator.backStacks.getValue(TopLevelDestination.Train),
                    sessionEpoch,
                    provider,
                ),
            BackStackId.Tab(TopLevelDestination.History) to
                rememberStackEntries(
                    BackStackId.Tab(TopLevelDestination.History),
                    navigator.backStacks.getValue(TopLevelDestination.History),
                    sessionEpoch,
                    provider,
                ),
            BackStackId.Tab(TopLevelDestination.Exercises) to
                rememberStackEntries(
                    BackStackId.Tab(TopLevelDestination.Exercises),
                    navigator.backStacks.getValue(TopLevelDestination.Exercises),
                    sessionEpoch,
                    provider,
                ),
            BackStackId.Tab(TopLevelDestination.Profile) to
                rememberStackEntries(
                    BackStackId.Tab(TopLevelDestination.Profile),
                    navigator.backStacks.getValue(TopLevelDestination.Profile),
                    sessionEpoch,
                    provider,
                ),
        )

    NavDisplay(
        entries = navigator.visibleStacks.flatMap { entriesByStack.getValue(it) },
        modifier = modifier,
        onBack = { navigator.goBack() },
    )
}

/**
 * Decorates [stackId]'s [backStack] with its own, freshly remembered saveable-state and
 * `ViewModelStore` decorators. Called unconditionally for every stack on every recomposition, so
 * navigation3 never sees an entry "leave" this decoration (the trigger for dropping its saved
 * state and ViewModels) just because [stackId] isn't the one currently shown.
 */
@Composable
private fun rememberStackEntries(
    stackId: BackStackId,
    backStack: List<NavKey>,
    sessionEpoch: Int,
    entryProvider: (VisibleNavKey) -> NavEntry<VisibleNavKey>,
): List<NavEntry<VisibleNavKey>> =
    rememberDecoratedNavEntries(
        backStack = backStack.mapIndexed { depth, key -> VisibleNavKey(key, stackId, depth, sessionEpoch) },
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
        entryProvider = entryProvider,
    )

/**
 * Re-keys the [NavEntry] [entryProvider] builds for a [VisibleNavKey.key] by its
 * [VisibleNavKey.contentKey], so the same [NavKey] twice in one stack (e.g. pushed, navigated away
 * from, then pushed again) still gets its own saveable state and `ViewModelStore` within that
 * stack's decorators.
 */
internal fun stackScoped(entryProvider: (NavKey) -> NavEntry<NavKey>): (VisibleNavKey) -> NavEntry<VisibleNavKey> =
    { visible ->
        val inner = entryProvider(visible.key)
        NavEntry(
            key = visible,
            contentKey = visible.contentKey,
            metadata = inner.metadata,
            content = { inner.Content() },
        )
    }
