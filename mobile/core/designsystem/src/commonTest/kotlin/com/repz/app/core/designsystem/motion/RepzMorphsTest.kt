package com.repz.app.core.designsystem.motion

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import kotlin.test.Test
import kotlin.test.assertTrue

private const val BOUNDS_TOLERANCE = 0.01f

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class RepzMorphsTest {
    @Test
    fun given_everyFixedMorphPair_whenReadingCubicsAtBothEnds_thenNeitherIsEmpty() {
        assertHasCubicsAtBothEnds(RepzMorphs.circleToBurst)
        assertHasCubicsAtBothEnds(RepzMorphs.sunnyToCircle)
        assertHasCubicsAtBothEnds(RepzMorphs.circleToGem)
        assertHasCubicsAtBothEnds(RepzMorphs.circleToFlower)
        assertHasCubicsAtBothEnds(RepzMorphs.flowerToCookie9Sided)
    }

    @Test
    fun given_checkboxRadiusAndSize_whenBuildingCircleToRoundedSquare_thenItBuildsAndStaysInsideTheUnitSquare() {
        val morph = RepzMorphs.circleToRoundedSquare(cornerRadius = 10.dp, size = 28.dp)

        assertHasCubicsAtBothEnds(morph)
        assertBoundsInsideUnitSquare(morph, progress = 0f)
        assertBoundsInsideUnitSquare(morph, progress = 1f)
    }

    @Test
    fun given_fabRadiusAndSize_whenBuildingRoundedSquareToCookie4Sided_thenItBuildsAndStaysInsideTheUnitSquare() {
        val morph = RepzMorphs.roundedSquareToCookie4Sided(cornerRadius = 16.dp, size = 56.dp)

        assertHasCubicsAtBothEnds(morph)
        assertBoundsInsideUnitSquare(morph, progress = 0f)
        assertBoundsInsideUnitSquare(morph, progress = 1f)
    }

    private fun assertHasCubicsAtBothEnds(morph: Morph) {
        assertTrue(morph.asCubics(0f).isNotEmpty())
        assertTrue(morph.asCubics(1f).isNotEmpty())
    }

    private fun assertBoundsInsideUnitSquare(
        morph: Morph,
        progress: Float,
    ) {
        val cubics = morph.asCubics(progress)
        val xs = cubics.flatMap { listOf(it.anchor0X, it.anchor1X, it.control0X, it.control1X) }
        val ys = cubics.flatMap { listOf(it.anchor0Y, it.anchor1Y, it.control0Y, it.control1Y) }

        assertTrue(xs.all { it >= -BOUNDS_TOLERANCE && it <= 1f + BOUNDS_TOLERANCE })
        assertTrue(ys.all { it >= -BOUNDS_TOLERANCE && it <= 1f + BOUNDS_TOLERANCE })
    }
}
