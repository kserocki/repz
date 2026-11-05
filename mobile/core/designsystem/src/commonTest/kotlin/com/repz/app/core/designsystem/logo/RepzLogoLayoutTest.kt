package com.repz.app.core.designsystem.logo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val SMALL_ARTWORK_WIDTH_PX = 24f
private const val SPLASH_ARTWORK_WIDTH_PX = 144f

class RepzLogoLayoutTest {
    @Test
    fun given_artworkAspectRatio_whenRead_thenMatchesArtworkBoundsWidthOverHeight() {
        val bounds = RepzLogoGeometry.artworkBounds
        assertEquals(bounds.width / bounds.height, RepzLogoGeometry.artworkAspectRatio)
    }

    @Test
    fun given_24dpArtwork_whenConvertingOuterLeft_thenItsLeftEdgeIsZero() {
        val rect = partRectPx(RepzLogoPart.OuterLeft, SMALL_ARTWORK_WIDTH_PX)
        assertEquals(0f, rect.left)
    }

    @Test
    fun given_24dpArtwork_whenConvertingOuterRight_thenItsRightEdgeIsTheArtworkWidth() {
        val rect = partRectPx(RepzLogoPart.OuterRight, SMALL_ARTWORK_WIDTH_PX)
        assertEquals(SMALL_ARTWORK_WIDTH_PX, rect.right, absoluteTolerance = 0.01f)
    }

    @Test
    fun given_144dpArtwork_whenConvertingOuterLeftAndRight_thenTheySpanTheFullArtworkWidth() {
        val left = partRectPx(RepzLogoPart.OuterLeft, SPLASH_ARTWORK_WIDTH_PX)
        val right = partRectPx(RepzLogoPart.OuterRight, SPLASH_ARTWORK_WIDTH_PX)
        assertEquals(0f, left.left)
        assertEquals(SPLASH_ARTWORK_WIDTH_PX, right.right, absoluteTolerance = 0.01f)
    }

    @Test
    fun given_aCentredSymbol_whenComputingPlateEntryDistance_thenEachPlateLandsExactlyOnItsScreenEdge() {
        assertDistancePutsPlatePastItsEdge(symbolLeftInWindowPx = 88f, artworkWidthPx = SPLASH_ARTWORK_WIDTH_PX, windowWidthPx = 320f)
    }

    @Test
    fun given_anOffCentreSymbol_whenComputingPlateEntryDistance_thenEachPlateStillLandsExactlyOnItsScreenEdge() {
        assertDistancePutsPlatePastItsEdge(symbolLeftInWindowPx = 10f, artworkWidthPx = SPLASH_ARTWORK_WIDTH_PX, windowWidthPx = 400f)
        assertDistancePutsPlatePastItsEdge(symbolLeftInWindowPx = -40f, artworkWidthPx = SPLASH_ARTWORK_WIDTH_PX, windowWidthPx = 390f)
    }

    @Test
    fun given_theBar_whenComputingPlateEntryDistance_thenItIsZero() {
        assertEquals(0f, plateEntryDistancePx(RepzLogoPart.Bar, 88f, SPLASH_ARTWORK_WIDTH_PX, 320f))
    }

    @Test
    fun given_leftParts_whenComputingPlateEntryDistance_thenItIsNegative() {
        for (part in listOf(RepzLogoPart.InnerLeft, RepzLogoPart.MiddleLeft, RepzLogoPart.OuterLeft)) {
            assertTrue(plateEntryDistancePx(part, 88f, SPLASH_ARTWORK_WIDTH_PX, 320f) < 0f, "$part should be negative")
        }
    }

    @Test
    fun given_rightParts_whenComputingPlateEntryDistance_thenItIsPositive() {
        for (part in listOf(RepzLogoPart.InnerRight, RepzLogoPart.MiddleRight, RepzLogoPart.OuterRight)) {
            assertTrue(plateEntryDistancePx(part, 88f, SPLASH_ARTWORK_WIDTH_PX, 320f) > 0f, "$part should be positive")
        }
    }

    @Test
    fun given_aPlateApproachingRestFromAbove_whenItCrossesZero_thenHasLanded() {
        assertTrue(hasLanded(previousEntry = 0.05f, currentEntry = -0.02f))
    }

    @Test
    fun given_aPlateStillAboveRest_whenNotYetCrossed_thenHasNotLanded() {
        assertFalse(hasLanded(previousEntry = 1f, currentEntry = 0.4f))
    }

    @Test
    fun given_aPlateAlreadyPastRest_whenStillBelowZero_thenHasNotLandedAgain() {
        assertFalse(hasLanded(previousEntry = -0.02f, currentEntry = -0.05f))
    }

    @Test
    fun given_aPlateOvershooting_whenItCrossesBackUpThroughZero_thenHasNotLanded() {
        assertFalse(hasLanded(previousEntry = -0.05f, currentEntry = 0.01f))
    }

    @Test
    fun given_noPreviousPositiveValue_whenAtExactlyZero_thenHasNotLanded() {
        assertFalse(hasLanded(previousEntry = 0f, currentEntry = 0f))
    }

    private fun assertDistancePutsPlatePastItsEdge(
        symbolLeftInWindowPx: Float,
        artworkWidthPx: Float,
        windowWidthPx: Float,
    ) {
        for (part in listOf(RepzLogoPart.InnerLeft, RepzLogoPart.MiddleLeft, RepzLogoPart.OuterLeft)) {
            val rect = partRectPx(part, artworkWidthPx)
            val distance = plateEntryDistancePx(part, symbolLeftInWindowPx, artworkWidthPx, windowWidthPx)
            assertEquals(
                0f,
                symbolLeftInWindowPx + rect.right + distance,
                absoluteTolerance = 0.01f,
                message = "$part should land on the left edge",
            )
        }
        for (part in listOf(RepzLogoPart.InnerRight, RepzLogoPart.MiddleRight, RepzLogoPart.OuterRight)) {
            val rect = partRectPx(part, artworkWidthPx)
            val distance = plateEntryDistancePx(part, symbolLeftInWindowPx, artworkWidthPx, windowWidthPx)
            assertEquals(
                windowWidthPx,
                symbolLeftInWindowPx + rect.left + distance,
                absoluteTolerance = 0.01f,
                message = "$part should land on the right edge",
            )
        }
    }
}

private fun assertEquals(
    expected: Float,
    actual: Float,
    absoluteTolerance: Float,
    message: String? = null,
) {
    assertTrue(kotlin.math.abs(expected - actual) <= absoluteTolerance, message ?: "expected $expected but was $actual")
}
