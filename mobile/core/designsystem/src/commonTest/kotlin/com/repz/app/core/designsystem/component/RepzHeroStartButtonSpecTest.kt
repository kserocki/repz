package com.repz.app.core.designsystem.component

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class RepzHeroStartButtonSpecTest {
    @Test
    fun given_heroButton_whenAtRest_thenContainerAndDiscAreFullyRounded() {
        assertEquals(88.dp, HeroHeight)
        assertEquals(44.dp, HeroHeight / 2, "rest container radius = height / 2")
        assertEquals(56.dp, HeroDiscSize)
        assertEquals(28.dp, HeroDiscSize / 2, "rest disc radius = disc size / 2")
    }

    @Test
    fun given_heroButton_whenPressed_thenContainerMorphsTo22AndDiscMorphsTo16() {
        assertEquals(22.dp, HeroPressedRadius)
        assertEquals(16.dp, HeroDiscPressedRadius)
    }

    @Test
    fun given_heroSplitButton_whenLeadingSegmentPressed_thenOnlyItsOuterCornersMorph() {
        val restOuter = HeroHeight / 2
        val rest =
            connectedGroupSegmentCorners(isFirst = true, isLast = false, outerRadius = restOuter, innerRadius = HeroSplitInnerRadius)
        val pressed =
            connectedGroupSegmentCorners(
                isFirst = true,
                isLast = false,
                outerRadius = HeroPressedRadius,
                innerRadius = HeroSplitInnerRadius,
            )

        assertEquals(RepzSegmentCornerRadii(restOuter, HeroSplitInnerRadius, HeroSplitInnerRadius, restOuter), rest)
        assertEquals(
            RepzSegmentCornerRadii(HeroPressedRadius, HeroSplitInnerRadius, HeroSplitInnerRadius, HeroPressedRadius),
            pressed,
        )
    }

    @Test
    fun given_heroSplitButton_whenTrailingSegmentPressed_thenOnlyItsOuterCornersMorph() {
        val restOuter = HeroHeight / 2
        val rest =
            connectedGroupSegmentCorners(isFirst = false, isLast = true, outerRadius = restOuter, innerRadius = HeroSplitInnerRadius)
        val pressed =
            connectedGroupSegmentCorners(
                isFirst = false,
                isLast = true,
                outerRadius = HeroPressedRadius,
                innerRadius = HeroSplitInnerRadius,
            )

        assertEquals(RepzSegmentCornerRadii(HeroSplitInnerRadius, restOuter, restOuter, HeroSplitInnerRadius), rest)
        assertEquals(
            RepzSegmentCornerRadii(HeroSplitInnerRadius, HeroPressedRadius, HeroPressedRadius, HeroSplitInnerRadius),
            pressed,
        )
    }

    @Test
    fun given_plainSplitButton_whenEitherSegmentPressed_thenOuterCornersMorphTo12AndInnerStayAt6() {
        val restOuter = SplitButtonHeight / 2
        assertEquals(20.dp, restOuter)

        val leadingRest =
            connectedGroupSegmentCorners(isFirst = true, isLast = false, outerRadius = restOuter, innerRadius = SplitButtonInnerRadius)
        val leadingPressed =
            connectedGroupSegmentCorners(
                isFirst = true,
                isLast = false,
                outerRadius = SplitButtonPressedOuterRadius,
                innerRadius = SplitButtonInnerRadius,
            )
        val trailingPressed =
            connectedGroupSegmentCorners(
                isFirst = false,
                isLast = true,
                outerRadius = SplitButtonPressedOuterRadius,
                innerRadius = SplitButtonInnerRadius,
            )

        assertEquals(RepzSegmentCornerRadii(restOuter, 6.dp, 6.dp, restOuter), leadingRest)
        assertEquals(RepzSegmentCornerRadii(12.dp, 6.dp, 6.dp, 12.dp), leadingPressed)
        assertEquals(RepzSegmentCornerRadii(6.dp, 12.dp, 12.dp, 6.dp), trailingPressed)
    }
}
