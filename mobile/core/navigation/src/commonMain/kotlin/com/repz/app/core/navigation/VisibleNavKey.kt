package com.repz.app.core.navigation

import androidx.navigation3.runtime.NavKey

/** Identifies which of [Navigator]'s back stacks a [VisibleNavKey] belongs to. */
sealed interface BackStackId {
    data object Auth : BackStackId

    data class Tab(
        val destination: TopLevelDestination,
    ) : BackStackId
}

/**
 * One entry of [Navigator.visibleEntries]: [key] tagged with the stack that owns it and its
 * [depth] there (its index within that stack, not its position in the visible list).
 */
data class VisibleNavKey(
    val key: NavKey,
    val stack: BackStackId,
    val depth: Int,
) {
    /** Unique per visible position, so the same key in Train and History (or twice in one stack) never collides. */
    val contentKey: String get() = "$stack/$depth/$key"
}
