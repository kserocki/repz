package com.repz.app.core.designsystem.logo

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val GRID_CENTER_X = 160f
private const val RATIO_TOLERANCE = 0.001f

class RepzLogoGeometryTest {
    @Test
    fun given_repzLogoGeometry_whenReadingParts_thenAllSevenPartsArePresent() {
        assertEquals(RepzLogoPart.entries.toSet(), RepzLogoGeometry.parts.keys)
    }

    @Test
    fun given_eachMirroredPair_whenComparingLeftAndRight_thenRightMirrorsLeftAroundGridCenter() {
        val pairs =
            listOf(
                RepzLogoPart.InnerLeft to RepzLogoPart.InnerRight,
                RepzLogoPart.MiddleLeft to RepzLogoPart.MiddleRight,
                RepzLogoPart.OuterLeft to RepzLogoPart.OuterRight,
            )
        for ((left, right) in pairs) {
            val leftRect = RepzLogoGeometry.parts.getValue(left)
            val rightRect = RepzLogoGeometry.parts.getValue(right)
            assertEquals(leftRect.y, rightRect.y, "y must match for $left/$right")
            assertEquals(leftRect.width, rightRect.width, "width must match for $left/$right")
            assertEquals(leftRect.height, rightRect.height, "height must match for $left/$right")
            // The right part's left edge mirrors the left part's left edge around x = 160.
            assertEquals(leftRect.x, GRID_CENTER_X * 2 - (rightRect.x + rightRect.width), "$left/$right should mirror around x = 160")
        }
    }

    @Test
    fun given_theThreePlatePairs_whenComparingHeights_thenTheyAreInRatio1To0_75To0_375() {
        val inner = RepzLogoGeometry.parts.getValue(RepzLogoPart.InnerLeft).height
        val middle = RepzLogoGeometry.parts.getValue(RepzLogoPart.MiddleLeft).height
        val outer = RepzLogoGeometry.parts.getValue(RepzLogoPart.OuterLeft).height

        assertRatio(0.75f, middle / inner, "middle:inner should be 0.75")
        assertRatio(0.375f, outer / inner, "outer:inner should be 0.375")
    }

    @Test
    fun given_adjacentPlates_whenMeasuringTheGapBetweenThem_thenItIsEightGridUnits() {
        val inner = RepzLogoGeometry.parts.getValue(RepzLogoPart.InnerLeft)
        val middle = RepzLogoGeometry.parts.getValue(RepzLogoPart.MiddleLeft)
        val outer = RepzLogoGeometry.parts.getValue(RepzLogoPart.OuterLeft)

        assertEquals(8f, inner.x - (middle.x + middle.width), "inner-middle gap")
        assertEquals(8f, middle.x - (outer.x + outer.width), "middle-outer gap")
    }
}

private fun assertRatio(
    expected: Float,
    actual: Float,
    message: String,
) {
    assertTrue(abs(expected - actual) <= RATIO_TOLERANCE, "$message: expected $expected but was $actual")
}
