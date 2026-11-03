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
 * One entry of [Navigator.visibleEntries]: [key] tagged with the stack that owns it, its [depth]
 * there (its index within that stack, not its position in the visible list), and the
 * [sessionEpoch] it was created in.
 */
data class VisibleNavKey(
    val key: NavKey,
    val stack: BackStackId,
    val depth: Int,
    val sessionEpoch: Int = 0,
) {
    /**
     * Unique per visible position within one session: the same key in Train and History (or
     * twice in one stack) never collides, and a sign-in or sign-out changes every entry's
     * [contentKey] (via [sessionEpoch]) even though [key], [stack] and [depth] are unchanged, so
     * navigation3 discards the previous session's saveable state and `ViewModelStore` for it.
     */
    val contentKey: String get() = "$sessionEpoch/$stack/$depth/$key"
}
