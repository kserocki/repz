package com.repz.app.core.designsystem.motion

import kotlin.test.Test
import kotlin.test.assertEquals

class RepzMotionSchemeTest {
    @Test
    fun given_notReduced_whenReadingEverySlot_thenMatchesTheCorrespondingRepzSpring() {
        val scheme = repzMotionScheme(reducedMotion = false)
        val springs = RepzSprings(reducedMotion = false)

        assertEquals(springs.spatialDefault<Float>(), scheme.defaultSpatialSpec())
        assertEquals(springs.spatialFast<Float>(), scheme.fastSpatialSpec())
        assertEquals(springs.spatialSlow<Float>(), scheme.slowSpatialSpec())
        assertEquals(springs.effectsDefault<Float>(), scheme.defaultEffectsSpec())
        assertEquals(springs.effectsFast<Float>(), scheme.fastEffectsSpec())
        assertEquals(springs.effectsSlow<Float>(), scheme.slowEffectsSpec())
    }

    @Test
    fun given_reduced_whenReadingEverySlot_thenMatchesTheCorrespondingRepzSpring() {
        val scheme = repzMotionScheme(reducedMotion = true)
        val springs = RepzSprings(reducedMotion = true)

        assertEquals(springs.spatialDefault<Float>(), scheme.defaultSpatialSpec())
        assertEquals(springs.spatialFast<Float>(), scheme.fastSpatialSpec())
        assertEquals(springs.spatialSlow<Float>(), scheme.slowSpatialSpec())
        assertEquals(springs.effectsDefault<Float>(), scheme.defaultEffectsSpec())
        assertEquals(springs.effectsFast<Float>(), scheme.fastEffectsSpec())
        assertEquals(springs.effectsSlow<Float>(), scheme.slowEffectsSpec())
    }
}
