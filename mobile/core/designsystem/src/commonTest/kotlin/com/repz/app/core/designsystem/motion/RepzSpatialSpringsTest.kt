package com.repz.app.core.designsystem.motion

import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RepzSpringsTest {
    @Test
    fun given_notReduced_whenReadingEverySpring_thenReturnsASpringSpecWithTheTokenValues() {
        val springs = RepzSprings(reducedMotion = false)

        assertEquals(
            SpringSpec<Float>(dampingRatio = RepzSpringTokens.spatialFast.dampingRatio, stiffness = RepzSpringTokens.spatialFast.stiffness),
            springs.spatialFast(),
        )
        assertEquals(
            SpringSpec<Float>(
                dampingRatio = RepzSpringTokens.effectsDefault.dampingRatio,
                stiffness = RepzSpringTokens.effectsDefault.stiffness,
            ),
            springs.effectsDefault(),
        )
        assertEquals(
            SpringSpec<Float>(dampingRatio = RepzSpringTokens.celebrate.dampingRatio, stiffness = RepzSpringTokens.celebrate.stiffness),
            springs.celebrate(),
        )
        assertEquals(
            SpringSpec<Float>(
                dampingRatio = RepzSpringTokens.logoWordmark.dampingRatio,
                stiffness = RepzSpringTokens.logoWordmark.stiffness,
            ),
            springs.logoWordmark(),
        )
    }

    @Test
    fun given_reduced_whenReadingASpatialSpring_thenReturnsASnapSpec() {
        val springs = RepzSprings(reducedMotion = true)

        assertIs<SnapSpec<Float>>(springs.spatialFast<Float>())
        assertIs<SnapSpec<Float>>(springs.spatialDefault<Float>())
        assertIs<SnapSpec<Float>>(springs.spatialSlow<Float>())
        assertIs<SnapSpec<Float>>(springs.celebrate<Float>())
        assertIs<SnapSpec<Float>>(springs.pressMorph<Float>())
        assertIs<SnapSpec<Float>>(springs.logoBar<Float>())
        assertIs<SnapSpec<Float>>(springs.logoPlate<Float>())
        assertIs<SnapSpec<Float>>(springs.logoPlatePr<Float>())
        assertIs<SnapSpec<Float>>(springs.logoImpact<Float>())
        assertIs<SnapSpec<Float>>(springs.logoWordmark<Float>())
    }

    @Test
    fun given_reduced_whenReadingAnEffectsSpring_thenReturnsA150MsTween() {
        val springs = RepzSprings(reducedMotion = true)
        val expected = TweenSpec<Float>(durationMillis = 150)

        assertEquals(expected, springs.effectsFast())
        assertEquals(expected, springs.effectsDefault())
        assertEquals(expected, springs.effectsSlow())
    }
}
