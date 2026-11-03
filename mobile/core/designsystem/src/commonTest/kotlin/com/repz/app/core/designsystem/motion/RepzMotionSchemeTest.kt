package com.repz.app.core.designsystem.motion

import kotlin.test.Test
import kotlin.test.assertEquals

class RepzMotionSchemeTest {
    @Test
    fun given_notReduced_whenReadingEverySlot_thenMatchesTheCorrespondingRepzSpring() {
        val scheme = repzMotionScheme(reducedMotion = false)
        val spatialSprings = RepzSpatialSprings(reducedMotion = false)
        val effectsSprings = RepzEffectsSprings(reducedMotion = false)

        assertEquals(spatialSprings.spatialDefault<Float>(), scheme.defaultSpatialSpec())
        assertEquals(spatialSprings.spatialFast<Float>(), scheme.fastSpatialSpec())
        assertEquals(spatialSprings.spatialSlow<Float>(), scheme.slowSpatialSpec())
        assertEquals(effectsSprings.effectsDefault<Float>(), scheme.defaultEffectsSpec())
        assertEquals(effectsSprings.effectsFast<Float>(), scheme.fastEffectsSpec())
        assertEquals(effectsSprings.effectsSlow<Float>(), scheme.slowEffectsSpec())
    }

    @Test
    fun given_reduced_whenReadingEverySlot_thenMatchesTheCorrespondingRepzSpring() {
        val scheme = repzMotionScheme(reducedMotion = true)
        val spatialSprings = RepzSpatialSprings(reducedMotion = true)
        val effectsSprings = RepzEffectsSprings(reducedMotion = true)

        assertEquals(spatialSprings.spatialDefault<Float>(), scheme.defaultSpatialSpec())
        assertEquals(spatialSprings.spatialFast<Float>(), scheme.fastSpatialSpec())
        assertEquals(spatialSprings.spatialSlow<Float>(), scheme.slowSpatialSpec())
        assertEquals(effectsSprings.effectsDefault<Float>(), scheme.defaultEffectsSpec())
        assertEquals(effectsSprings.effectsFast<Float>(), scheme.fastEffectsSpec())
        assertEquals(effectsSprings.effectsSlow<Float>(), scheme.slowEffectsSpec())
    }
}
