package com.repz.app.core.designsystem.motion

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.material3.MotionScheme

/** [MotionScheme]'s six slots map 1:1 to the six M3-named `RepzSprings` tokens, so `RepzTheme`'s
 * `MaterialExpressiveTheme(motionScheme = …)` makes M3 components (switches, buttons, menus …)
 * follow the same tokens, and the same reduced-motion rule, as the rest of the app. */
private fun buildRepzMotionScheme(reducedMotion: Boolean): MotionScheme {
    val springs = RepzSprings(reducedMotion)
    return object : MotionScheme {
        override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = springs.spatialDefault()

        override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = springs.spatialFast()

        override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = springs.spatialSlow()

        override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = springs.effectsDefault()

        override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = springs.effectsFast()

        override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = springs.effectsSlow()
    }
}

private val StandardRepzMotionScheme = buildRepzMotionScheme(reducedMotion = false)
private val ReducedRepzMotionScheme = buildRepzMotionScheme(reducedMotion = true)

internal fun repzMotionScheme(reducedMotion: Boolean): MotionScheme =
    if (reducedMotion) ReducedRepzMotionScheme else StandardRepzMotionScheme
