package com.repz.app.core.designsystem.motion

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import kotlin.test.Test
import kotlin.test.assertEquals

class RepzEffectsSpringsTest {
    @Test
    fun given_notReduced_whenReadingEverySpring_thenReturnsASpringSpecWithTheTokenValues() {
        val springs = RepzEffectsSprings(reducedMotion = false)

        assertEquals(
            SpringSpec<Float>(
                dampingRatio = RepzSpringTokens.effectsDefault.dampingRatio,
                stiffness = RepzSpringTokens.effectsDefault.stiffness,
            ),
            springs.effectsDefault(),
        )
        assertEquals(
            SpringSpec<Float>(dampingRatio = RepzSpringTokens.effectsFast.dampingRatio, stiffness = RepzSpringTokens.effectsFast.stiffness),
            springs.effectsFast(),
        )
        assertEquals(
            SpringSpec<Float>(dampingRatio = RepzSpringTokens.effectsSlow.dampingRatio, stiffness = RepzSpringTokens.effectsSlow.stiffness),
            springs.effectsSlow(),
        )
    }

    @Test
    fun given_reduced_whenReadingAnySpring_thenReturnsA150MsTween() {
        val springs = RepzEffectsSprings(reducedMotion = true)
        val expected = TweenSpec<Float>(durationMillis = 150)

        assertEquals(expected, springs.effectsFast())
        assertEquals(expected, springs.effectsDefault())
        assertEquals(expected, springs.effectsSlow())
    }
}
