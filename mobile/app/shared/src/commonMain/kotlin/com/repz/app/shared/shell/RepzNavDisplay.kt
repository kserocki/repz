package com.repz.app.shared.shell

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.repz.app.core.navigation.Navigator
import com.repz.app.core.navigation.VisibleNavKey

/** Hosts [navigator]'s [Navigator.visibleEntries] through [repzEntries] (§3.6). */
@Composable
internal fun RepzNavDisplay(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    NavDisplay(
        backStack = navigator.visibleEntries,
        modifier = modifier,
        onBack = { navigator.goBack() },
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
        entryProvider = stackScoped(entryProvider { repzEntries() }),
    )
}

/**
 * Re-keys the [NavEntry] [entryProvider] builds for a [VisibleNavKey.key] by its
 * [VisibleNavKey.contentKey], so the same [NavKey] visible twice at once (a tab's own root plus
 * the selected tab, or twice in the same stack) still gets its own saveable state and
 * `ViewModelStore` (§3.2 "duplicate keys").
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
