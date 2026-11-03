package com.repz.app.core.designsystem.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** MOTION.md §2's plain duration tokens (no reduced-motion logic: see [RepzSpatialSprings]/
 * [RepzEffectsSprings]/[RepzTweens] for the tokens that are). */
object RepzDurations {
    val short: Duration = 100.milliseconds
    val medium: Duration = 200.milliseconds
    val pathDraw: Duration = 280.milliseconds
    val chartDraw: Duration = 600.milliseconds
    val restRingStep: Duration = 1.seconds
    val particleLifeInline: Duration = 600.milliseconds
    val particleLifeSummary: Duration = 1200.milliseconds
    val celebrationMax: Duration = 1500.milliseconds
    val shimmer: Duration = 1200.milliseconds
    val reducedMotionFade: Duration = 150.milliseconds
}

/** MOTION.md §2's easing curves. */
object RepzEasing {
    val emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val emphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val linear: Easing = LinearEasing
}

/** MOTION.md §2's stagger tokens. [LIST_MAX_ITEMS] is `const` (and so SCREAMING_SNAKE_CASE, unlike
 * its `RepzMotion.stagger.*` siblings) because it is a plain compile-time `Int`, not a `Duration`. */
object RepzStagger {
    val logoPlatesPerPair: Duration = 110.milliseconds
    val listPerItem: Duration = 30.milliseconds
    const val LIST_MAX_ITEMS: Int = 8
    val summaryPerBlock: Duration = 70.milliseconds
    val heatmapPerDiagonal: Duration = 12.milliseconds
    val digitsPerDigit: Duration = 20.milliseconds
}
