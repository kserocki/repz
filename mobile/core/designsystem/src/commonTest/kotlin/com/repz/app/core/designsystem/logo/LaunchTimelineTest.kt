package com.repz.app.core.designsystem.logo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class LaunchTimelineTest {
    @Test
    fun given_launchTimeline_whenRead_thenOffsetsMatchBrandLogoMdSection5() {
        val steps = launchTimeline()

        assertEquals(
            listOf(
                LaunchStage.Bar to 0.milliseconds,
                LaunchStage.InnerPlates to 120.milliseconds,
                LaunchStage.MiddlePlates to 230.milliseconds,
                LaunchStage.OuterPlates to 340.milliseconds,
                LaunchStage.Wordmark to 600.milliseconds,
            ),
            steps.map { it.stage to it.startAt },
        )
    }

    @Test
    fun given_eachPlateStage_whenReadingItsPair_thenItMatchesItsLandingOrder() {
        assertEquals(RepzPlatePair.Inner, LaunchStage.InnerPlates.pair)
        assertEquals(RepzPlatePair.Middle, LaunchStage.MiddlePlates.pair)
        assertEquals(RepzPlatePair.Outer, LaunchStage.OuterPlates.pair)
    }

    @Test
    fun given_barAndWordmarkStages_whenReadingTheirPair_thenItIsNull() {
        assertEquals(null, LaunchStage.Bar.pair)
        assertEquals(null, LaunchStage.Wordmark.pair)
    }
}
