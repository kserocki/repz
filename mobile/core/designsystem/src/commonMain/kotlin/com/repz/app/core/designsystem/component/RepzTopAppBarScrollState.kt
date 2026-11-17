package com.repz.app.core.designsystem.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll

/**
 * Wraps M3's [TopAppBarScrollBehavior] (`exitUntilCollapsed`, T5) so [RepzMediumTopAppBar] and
 * [RepzLargeTopAppBar] can collapse on scroll without features importing Material3 themselves
 * (AGENTS.md §3.7).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Stable
class RepzTopAppBarScrollState internal constructor(
    internal val behavior: TopAppBarScrollBehavior,
)

/** Remembers a [RepzTopAppBarScrollState] driving a [RepzMediumTopAppBar] or [RepzLargeTopAppBar]
 * that collapses to the small bar height as [Modifier.repzTopAppBarScroll]'s content scrolls. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberRepzTopAppBarScrollState(): RepzTopAppBarScrollState {
    val behavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(state = rememberTopAppBarState())
    return remember(behavior) { RepzTopAppBarScrollState(behavior) }
}

/** Connects [state] to the nested-scroll hierarchy: apply to the container that holds both the top
 * app bar and its scrolling content, the same placement M3's own `Scaffold` sample uses. */
@OptIn(ExperimentalMaterial3Api::class)
fun Modifier.repzTopAppBarScroll(state: RepzTopAppBarScrollState): Modifier = nestedScroll(state.behavior.nestedScrollConnection)
