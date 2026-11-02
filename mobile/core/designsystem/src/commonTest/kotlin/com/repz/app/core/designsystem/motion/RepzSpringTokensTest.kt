package com.repz.app.core.designsystem.motion

import kotlin.test.Test
import kotlin.test.assertEquals

class RepzSpringTokensTest {
    @Test
    fun given_everySpringToken_whenReadingDampingRatioAndStiffness_thenMatchesMotionDoc() {
        assertToken(RepzSpringTokens.spatialFast, dampingRatio = 0.9f, stiffness = 1400f)
        assertToken(RepzSpringTokens.spatialDefault, dampingRatio = 0.9f, stiffness = 700f)
        assertToken(RepzSpringTokens.spatialSlow, dampingRatio = 0.9f, stiffness = 300f)
        assertToken(RepzSpringTokens.effectsFast, dampingRatio = 1.0f, stiffness = 3800f)
        assertToken(RepzSpringTokens.effectsDefault, dampingRatio = 1.0f, stiffness = 1600f)
        assertToken(RepzSpringTokens.effectsSlow, dampingRatio = 1.0f, stiffness = 800f)
        assertToken(RepzSpringTokens.celebrate, dampingRatio = 0.55f, stiffness = 380f)
        assertToken(RepzSpringTokens.pressMorph, dampingRatio = 0.6f, stiffness = 1200f)
        assertToken(RepzSpringTokens.logoBar, dampingRatio = 0.8f, stiffness = 600f)
        assertToken(RepzSpringTokens.logoPlate, dampingRatio = 0.45f, stiffness = 500f)
        assertToken(RepzSpringTokens.logoPlatePr, dampingRatio = 0.4f, stiffness = 500f)
        assertToken(RepzSpringTokens.logoImpact, dampingRatio = 0.5f, stiffness = 800f)
        assertToken(RepzSpringTokens.logoWordmark, dampingRatio = 1.0f, stiffness = 380f)
    }

    @Test
    fun given_everySpringToken_whenReadingKind_thenOnlyEffectsTokensAreEffectsKind() {
        val spatialTokens =
            listOf(
                RepzSpringTokens.spatialFast,
                RepzSpringTokens.spatialDefault,
                RepzSpringTokens.spatialSlow,
                RepzSpringTokens.celebrate,
                RepzSpringTokens.pressMorph,
                RepzSpringTokens.logoBar,
                RepzSpringTokens.logoPlate,
                RepzSpringTokens.logoPlatePr,
                RepzSpringTokens.logoImpact,
                RepzSpringTokens.logoWordmark,
            )
        val effectsTokens = listOf(RepzSpringTokens.effectsFast, RepzSpringTokens.effectsDefault, RepzSpringTokens.effectsSlow)

        spatialTokens.forEach { assertEquals(RepzSpringKind.Spatial, it.kind) }
        effectsTokens.forEach { assertEquals(RepzSpringKind.Effects, it.kind) }
    }

    private fun assertToken(
        token: RepzSpringToken,
        dampingRatio: Float,
        stiffness: Float,
    ) {
        assertEquals(dampingRatio, token.dampingRatio)
        assertEquals(stiffness, token.stiffness)
    }
}
