package com.repz.app.core.designsystem.component

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class PressMorphCornerSizeTest {
    @Test
    fun given_restFraction_whenComputingRadius_thenHalfOfTheMeasuredDimension() {
        assertEquals(50f, pressMorphCornerRadiusPx(restDimensionPx = 100f, pressedRadiusPx = 12f, pressFraction = 0f))
    }

    @Test
    fun given_pressedFraction_whenComputingRadius_thenThePressedRadius() {
        assertEquals(12f, pressMorphCornerRadiusPx(restDimensionPx = 100f, pressedRadiusPx = 12f, pressFraction = 1f))
    }

    @Test
    fun given_halfwayFraction_whenComputingRadius_thenLerpsBetweenRestAndPressed() {
        assertEquals(31f, pressMorphCornerRadiusPx(restDimensionPx = 100f, pressedRadiusPx = 12f, pressFraction = 0.5f))
    }

    @Test
    fun given_outOfRangeFraction_whenComputingRadius_thenClampedToRestOrPressed() {
        assertEquals(50f, pressMorphCornerRadiusPx(restDimensionPx = 100f, pressedRadiusPx = 12f, pressFraction = -1f))
        assertEquals(12f, pressMorphCornerRadiusPx(restDimensionPx = 100f, pressedRadiusPx = 12f, pressFraction = 2f))
    }

    @Test
    fun given_aGrownHeight_whenAtRest_thenTheRestRadiusFollowsTheMeasuredSizeNotAToken() {
        // The regression this exists for: a label that wraps past the size token's minimum height
        // (e.g. 112 dp instead of a 96 dp token) must still render as a true pill, not a rounded
        // rectangle using the stale token-based half-height.
        val cornerSize = PressMorphCornerSize(pressedRadius = 12.dp, pressFraction = 0f)
        val grownSize = Size(width = 300f, height = 112f)

        assertEquals(56f, cornerSize.toPx(grownSize, Density(1f)))
    }

    @Test
    fun given_pressed_whenReadingCornerSize_thenUsesThePressedRadiusRegardlessOfMeasuredSize() {
        val cornerSize = PressMorphCornerSize(pressedRadius = 12.dp, pressFraction = 1f)
        val grownSize = Size(width = 300f, height = 112f)

        assertEquals(12f, cornerSize.toPx(grownSize, Density(1f)))
    }
}
