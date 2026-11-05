package com.repz.app.core.designsystem.logo

import kotlin.math.floor

/** How many of the 3 pairs are locked at [progress] (coerced to `0f..1f`; `NaN` coerces to itself
 * and floors to `0`, so it is treated as "nothing locked yet"): one pair locks per third. */
internal fun lockedPairCount(progress: Float): Int {
    val clamped = progress.coerceIn(0f, 1f)
    return floor(clamped * RepzPlatePair.entries.size).toInt()
}

/** Every pair's [PairState] at [progress]: the first [lockedPairCount] pairs (in [RepzPlatePair]'s
 * landing order) are [PairState.Locked], the next one is [PairState.Loading], the rest
 * [PairState.Pending]. At `progress = 1f` every pair is locked and none is [PairState.Loading]. */
internal fun pairStates(progress: Float): Map<RepzPlatePair, PairState> {
    val locked = lockedPairCount(progress)
    return RepzPlatePair.entries.associateWith { pair ->
        when {
            pair.ordinal < locked -> PairState.Locked
            pair.ordinal == locked -> PairState.Loading
            else -> PairState.Pending
        }
    }
}

/** A plate pair's [entry] (0 = rest, 1 = at its entry start) and [plateAlpha] before any animation
 * has run. */
internal data class LoaderPlatePose(
    val entry: Float,
    val plateAlpha: Float,
)

/** Every pair's pose before the indeterminate loop's first (or next) staggered entrance: off its
 * rest slot and fully hidden, so a pair still waiting for its own turn is never drawn solid at the
 * wrong, off-slot position — not even for the one frame before its delayed entrance starts. */
internal fun initialLoaderPlatePose(): LoaderPlatePose = LoaderPlatePose(entry = 1f, plateAlpha = 0f)
