package com.repz.app.core.designsystem.component

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class RepzConnectedButtonGroupSpecTest {
    private val outer = 20.dp
    private val inner = 6.dp

    @Test
    fun given_twoOptionGroup_whenReadingCorners_thenEachOptionHasOneOuterSide() {
        val first = connectedGroupSegmentCorners(isFirst = true, isLast = false, outerRadius = outer, innerRadius = inner)
        val last = connectedGroupSegmentCorners(isFirst = false, isLast = true, outerRadius = outer, innerRadius = inner)

        assertEquals(RepzSegmentCornerRadii(outer, inner, inner, outer), first)
        assertEquals(RepzSegmentCornerRadii(inner, outer, outer, inner), last)
    }

    @Test
    fun given_middleOptionInAThreeOptionGroup_whenReadingCorners_thenEveryCornerIsInner() {
        val middle = connectedGroupSegmentCorners(isFirst = false, isLast = false, outerRadius = outer, innerRadius = inner)

        assertEquals(RepzSegmentCornerRadii(inner, inner, inner, inner), middle)
    }

    @Test
    fun given_singleOptionGroup_whenReadingCorners_thenBothSidesAreOuter() {
        val only = connectedGroupSegmentCorners(isFirst = true, isLast = true, outerRadius = outer, innerRadius = inner)

        assertEquals(RepzSegmentCornerRadii(outer, outer, outer, outer), only)
    }

    @Test
    fun given_pressedOuterRadius_whenReadingFirstOption_thenOnlyTheOuterSideUsesIt() {
        val pressedOuter = 12.dp
        val pressed = connectedGroupSegmentCorners(isFirst = true, isLast = false, outerRadius = pressedOuter, innerRadius = inner)

        assertEquals(RepzSegmentCornerRadii(pressedOuter, inner, inner, pressedOuter), pressed)
    }
}
