package com.repz.app.core.designsystem.motion

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import kotlin.time.Duration

/**
 * MOTION.md §2's spatial springs (`RepzSpringKind.Spatial`: shape or position, never colour/alpha)
 * as reduced-motion-aware [FiniteAnimationSpec] factories, one per `RepzMotion.springs.*` token.
 * With [reducedMotion], every spring here `snap()`s — MOTION.md §2 "Reduced motion": "Spatial
 * springs snap instantly".
 */
@Stable
class RepzSpatialSprings internal constructor(
    private val reducedMotion: Boolean,
) {
    fun <T> spatialFast(): FiniteAnimationSpec<T> = specFor(RepzSpringTokens.spatialFast)

    fun <T> spatialDefault(): FiniteAnimationSpec<T> = specFor(RepzSpringTokens.spatialDefault)

    fun <T> spatialSlow(): FiniteAnimationSpec<T> = specFor(RepzSpringTokens.spatialSlow)

    fun <T> celebrate(): FiniteAnimationSpec<T> = specFor(RepzSpringTokens.celebrate)

    fun <T> pressMorph(): FiniteAnimationSpec<T> = specFor(RepzSpringTokens.pressMorph)

    fun <T> logoBar(): FiniteAnimationSpec<T> = specFor(RepzSpringTokens.logoBar)

    fun <T> logoPlate(): FiniteAnimationSpec<T> = specFor(RepzSpringTokens.logoPlate)

    fun <T> logoPlatePr(): FiniteAnimationSpec<T> = specFor(RepzSpringTokens.logoPlatePr)

    fun <T> logoImpact(): FiniteAnimationSpec<T> = specFor(RepzSpringTokens.logoImpact)

    fun <T> logoWordmark(): FiniteAnimationSpec<T> = specFor(RepzSpringTokens.logoWordmark)

    private fun <T> specFor(token: RepzSpringToken): FiniteAnimationSpec<T> =
        if (!reducedMotion) spring(dampingRatio = token.dampingRatio, stiffness = token.stiffness) else snap()
}

/**
 * MOTION.md §2's effects springs (`RepzSpringKind.Effects`: colour or alpha only) as reduced-
 * motion-aware [FiniteAnimationSpec] factories, one per `RepzMotion.effectsSprings.*` token. With
 * [reducedMotion], every spring here becomes a [RepzDurations.reducedMotionFade] crossfade —
 * MOTION.md §2 "Reduced motion": "Effects become a 150 ms crossfade".
 */
@Stable
class RepzEffectsSprings internal constructor(
    private val reducedMotion: Boolean,
) {
    fun <T> effectsFast(): FiniteAnimationSpec<T> = specFor(RepzSpringTokens.effectsFast)

    fun <T> effectsDefault(): FiniteAnimationSpec<T> = specFor(RepzSpringTokens.effectsDefault)

    fun <T> effectsSlow(): FiniteAnimationSpec<T> = specFor(RepzSpringTokens.effectsSlow)

    private fun <T> specFor(token: RepzSpringToken): FiniteAnimationSpec<T> =
        if (!reducedMotion) {
            spring(dampingRatio = token.dampingRatio, stiffness = token.stiffness)
        } else {
            tween(RepzDurations.reducedMotionFade.inWholeMilliseconds.toInt())
        }
}

/**
 * MOTION.md §2's 4 tweens (`RepzMotion.tweens.*`), reduced-motion-aware per the table in MOTION.md
 * §2 "Reduced motion": [short] is already a fade and is unchanged; [medium] shortens to
 * [RepzDurations.reducedMotionFade]; the draw tweens ([pathDraw], [chartDraw]) `snap()`.
 */
@Stable
class RepzTweens internal constructor(
    private val reducedMotion: Boolean,
) {
    fun <T> short(): FiniteAnimationSpec<T> = tween(RepzDurations.short.inWholeMilliseconds.toInt())

    fun <T> medium(): FiniteAnimationSpec<T> {
        val duration = if (reducedMotion) RepzDurations.reducedMotionFade else RepzDurations.medium
        return tween(duration.inWholeMilliseconds.toInt())
    }

    fun <T> pathDraw(): FiniteAnimationSpec<T> = drawTween(RepzDurations.pathDraw)

    fun <T> chartDraw(): FiniteAnimationSpec<T> = drawTween(RepzDurations.chartDraw)

    private fun <T> drawTween(duration: Duration): FiniteAnimationSpec<T> =
        if (reducedMotion) {
            snap()
        } else {
            tween(durationMillis = duration.inWholeMilliseconds.toInt(), easing = RepzEasing.emphasizedDecelerate)
        }
}
