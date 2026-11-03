package com.repz.app.core.designsystem.motion

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.material3.MotionScheme

/** [MotionScheme]'s six slots map 1:1 to the six M3-named `RepzSpatialSprings`/`RepzEffectsSprings`
 * tokens, so `RepzTheme`'s `MaterialExpressiveTheme(motionScheme = …)` makes M3 components
 * (switches, buttons, menus …) follow the same tokens, and the same reduced-motion rule, as the
 * rest of the app. */
private fun buildRepzMotionScheme(reducedMotion: Boolean): MotionScheme {
    val spatialSprings = RepzSpatialSprings(reducedMotion)
    val effectsSprings = RepzEffectsSprings(reducedMotion)
    return object : MotionScheme {
        override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = spatialSprings.spatialDefault()

        override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = spatialSprings.spatialFast()

        override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = spatialSprings.spatialSlow()

        override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = effectsSprings.effectsDefault()

        override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = effectsSprings.effectsFast()

        override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = effectsSprings.effectsSlow()
    }
}

private val StandardRepzMotionScheme = buildRepzMotionScheme(reducedMotion = false)
private val ReducedRepzMotionScheme = buildRepzMotionScheme(reducedMotion = true)

internal fun repzMotionScheme(reducedMotion: Boolean): MotionScheme =
    if (reducedMotion) ReducedRepzMotionScheme else StandardRepzMotionScheme
