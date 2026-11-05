package com.repz.app.core.designsystem.logo

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LaunchLogoLayoutTest {
    @Test
    fun given_anySymbolWidth_whenComputingLayout_thenSymbolHeightMatchesTheArtworkAspectRatio() {
        for (symbolWidth in listOf(24.dp, 48.dp, 96.dp, 144.dp)) {
            val layout = launchLogoLayout(symbolWidth)
            assertEquals(symbolWidth / RepzLogoGeometry.artworkAspectRatio, layout.symbolHeight)
        }
    }

    @Test
    fun given_anySymbolWidth_whenComputingLayout_thenTheSymbolIsTallerThanTheUnoffsetWordmark() {
        // RepzLaunchLogo's Box relies on this: the symbol must be the box's only sizing child at
        // every width, or the wordmark would inflate the box's reported size and shift the
        // symbol's centre away from the OS splash icon's position again.
        for (symbolWidth in listOf(24.dp, 48.dp, 96.dp, 144.dp, 288.dp)) {
            val layout = launchLogoLayout(symbolWidth)
            val wordmarkHeight = layout.wordmarkWidth / layout.wordmarkAspectRatio
            assertTrue(
                wordmarkHeight < layout.symbolHeight,
                "wordmark height $wordmarkHeight should be less than symbol height ${layout.symbolHeight} at $symbolWidth",
            )
        }
    }

    @Test
    fun given_the144dpSplashWidth_whenComputingLayout_thenValuesMatchBrandLogoMdSection2() {
        val layout = launchLogoLayout(144.dp)
        assertEqualsWithinTolerance(62.27.dp, layout.symbolHeight)
        assertEqualsWithinTolerance(11.68.dp, layout.gapBelowPlates)
        assertEqualsWithinTolerance(119.19.dp, layout.wordmarkWidth)
    }
}

private fun assertEqualsWithinTolerance(
    expected: Dp,
    actual: Dp,
    tolerance: Dp = 0.01.dp,
) {
    assertTrue(abs(expected.value - actual.value) <= tolerance.value, "expected $expected but was $actual")
}
