package com.repz.app.core.designsystem.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Whether the current composition should prefer the reduced-motion fallbacks (MOTION.md §2):
 * springs snap, draw tweens snap, effects/`tweens.medium` shorten to a crossfade. Fed by
 * `RepzTheme`, from either its own `reducedMotion` parameter or [rememberSystemReducedMotion] when
 * that parameter is `null`. `internal` on purpose: MOTION.md §2 says "features never check it
 * themselves", so only `core:designsystem` code (including [RepzMotion]) can read it.
 */
internal val LocalReducedMotion = staticCompositionLocalOf { false }

private val NormalRepzSpatialSprings = RepzSpatialSprings(reducedMotion = false)
private val ReducedRepzSpatialSprings = RepzSpatialSprings(reducedMotion = true)
private val NormalRepzEffectsSprings = RepzEffectsSprings(reducedMotion = false)
private val ReducedRepzEffectsSprings = RepzEffectsSprings(reducedMotion = true)
private val NormalRepzTweens = RepzTweens(reducedMotion = false)
private val ReducedRepzTweens = RepzTweens(reducedMotion = true)

/**
 * `core:designsystem`'s one motion API (MOTION.md §2, DESIGN_SYSTEM §2 `motion.*`): reduced-motion-
 * aware spring/tween factories, plus the plain duration, easing and stagger tokens. Callers never
 * write `spring(`/`tween(` themselves and never read [LocalReducedMotion].
 */
object RepzMotion {
    val springs: RepzSpatialSprings
        @Composable @ReadOnlyComposable
        get() = if (LocalReducedMotion.current) ReducedRepzSpatialSprings else NormalRepzSpatialSprings

    val effectsSprings: RepzEffectsSprings
        @Composable @ReadOnlyComposable
        get() = if (LocalReducedMotion.current) ReducedRepzEffectsSprings else NormalRepzEffectsSprings

    val tweens: RepzTweens
        @Composable @ReadOnlyComposable
        get() = if (LocalReducedMotion.current) ReducedRepzTweens else NormalRepzTweens

    val durations: RepzDurations get() = RepzDurations
    val easing: RepzEasing get() = RepzEasing
    val stagger: RepzStagger get() = RepzStagger
}
