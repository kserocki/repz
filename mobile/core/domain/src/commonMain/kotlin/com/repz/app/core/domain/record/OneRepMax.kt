package com.repz.app.core.domain.record

import com.repz.app.core.domain.model.Weight

// Epley's 1RM = weight * (1 + reps / EPLEY_DENOMINATOR); the rounding offset is half the
// denominator, the standard trick for rounding integer division half up.
private const val EPLEY_DENOMINATOR = 30
private const val EPLEY_ROUNDING_OFFSET = EPLEY_DENOMINATOR / 2

/**
 * Epley's estimated one-rep max, in integer grams: `(grams * (30 + reps) + 15) / 30`, the `+ 15`
 * rounding the integer division half up to the nearest gram. `reps == 1` is the identity (a real
 * single is its own 1RM) rather than going through the formula, which would not return the exact
 * weight for every gram value.
 */
fun estimateOneRepMax(
    weight: Weight,
    reps: Int,
): Weight {
    require(reps >= 1) { "Reps must be at least 1 to estimate a one-rep max: $reps" }
    if (reps == 1) return weight

    val numerator = weight.grams * (EPLEY_DENOMINATOR + reps) + EPLEY_ROUNDING_OFFSET
    return Weight(numerator / EPLEY_DENOMINATOR)
}
