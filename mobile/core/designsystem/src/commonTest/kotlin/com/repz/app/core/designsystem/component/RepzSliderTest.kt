package com.repz.app.core.designsystem.component

import kotlin.test.Test
import kotlin.test.assertEquals

class RepzSliderTest {
    @Test
    fun given_continuousSlider_whenReadingStepIndexAtAnyValue_thenAlwaysZero() {
        assertEquals(0, sliderStepIndex(value = 0f, valueRange = 0f..1f, steps = 0))
        assertEquals(0, sliderStepIndex(value = 0.5f, valueRange = 0f..1f, steps = 0))
        assertEquals(0, sliderStepIndex(value = 1f, valueRange = 0f..1f, steps = 0))
    }

    @Test
    fun given_steppedSlider_whenReadingStepIndexAtTheEnds_thenFirstAndLastPosition() {
        assertEquals(0, sliderStepIndex(value = 0f, valueRange = 0f..10f, steps = 4))
        assertEquals(5, sliderStepIndex(value = 10f, valueRange = 0f..10f, steps = 4))
    }

    @Test
    fun given_steppedSlider_whenReadingStepIndexAtEachEvenlySpacedPosition_thenMatchesItsIndex() {
        // 4 steps between 0 and 10 land on 0, 2, 4, 6, 8, 10 (M3's own StepsSlider example).
        assertEquals(1, sliderStepIndex(value = 2f, valueRange = 0f..10f, steps = 4))
        assertEquals(2, sliderStepIndex(value = 4f, valueRange = 0f..10f, steps = 4))
        assertEquals(3, sliderStepIndex(value = 6f, valueRange = 0f..10f, steps = 4))
        assertEquals(4, sliderStepIndex(value = 8f, valueRange = 0f..10f, steps = 4))
    }

    @Test
    fun given_steppedSlider_whenValueIsBetweenTwoPositions_thenRoundsToTheNearestOne() {
        // Positions are 2 apart (0, 2, 4, ...); the midpoint between position 1 (2) and position 2
        // (4) is 3, so 2.4 is still nearest position 1 and 3.2 is already nearest position 2.
        assertEquals(1, sliderStepIndex(value = 2.4f, valueRange = 0f..10f, steps = 4))
        assertEquals(2, sliderStepIndex(value = 3.2f, valueRange = 0f..10f, steps = 4))
    }

    @Test
    fun given_restTimerStepRange_whenReadingStepIndexForEvery15SecondStep_thenMatchesItsPosition() {
        // components-controls: 0 to 5:00 (300 s) in 15 s steps -> 19 intermediate steps.
        assertEquals(0, sliderStepIndex(value = 0f, valueRange = 0f..300f, steps = 19))
        assertEquals(10, sliderStepIndex(value = 150f, valueRange = 0f..300f, steps = 19))
        assertEquals(20, sliderStepIndex(value = 300f, valueRange = 0f..300f, steps = 19))
    }
}
