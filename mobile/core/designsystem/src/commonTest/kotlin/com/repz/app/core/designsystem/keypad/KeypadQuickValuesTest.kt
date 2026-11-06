package com.repz.app.core.designsystem.keypad

import com.repz.app.core.designsystem.format.FixedDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

private val NO_MIN = FixedDecimal.ZERO
private val KG_MAX = FixedDecimal(100_000) // 1000.00 kg
private val LB_MAX = FixedDecimal(220_462) // 2204.62 lb

class KeypadQuickValuesTest {
    @Test
    fun givenAnOnGridBase_whenBuildingWeightSuggestionsWithA2Dot5Step_thenTheyAreTheFourStepsAroundIt() {
        val suggestions = KeypadQuickValues.weightSuggestions(FixedDecimal(8_250), FixedDecimal(250), NO_MIN, KG_MAX)

        assertEquals(listOf(7_750L, 8_000L, 8_500L, 8_750L).map { FixedDecimal(it) }, suggestions)
    }

    @Test
    fun givenABaseNearTheMinimum_whenBuildingWeightSuggestions_thenValuesBelowTheMinimumAreDropped() {
        val suggestions = KeypadQuickValues.weightSuggestions(FixedDecimal(250), FixedDecimal(250), NO_MIN, KG_MAX)

        // base - 2 steps (-2.5) is below 0 and dropped; the other three stay.
        assertEquals(listOf(0L, 500L, 750L).map { FixedDecimal(it) }, suggestions)
    }

    @Test
    fun givenAnOffGridBase_whenBuildingWeightSuggestionsWithA5Step_thenEachCandidateRoundsToTheNearestStep() {
        val suggestions = KeypadQuickValues.weightSuggestions(FixedDecimal(8_250), FixedDecimal(500), NO_MIN, KG_MAX)

        assertEquals(listOf(7_500L, 8_000L, 9_000L, 9_500L).map { FixedDecimal(it) }, suggestions)
    }

    @Test
    fun givenABaseNearTheMaximum_whenBuildingWeightSuggestions_thenValuesAboveTheMaximumAreDropped() {
        val suggestions = KeypadQuickValues.weightSuggestions(KG_MAX, FixedDecimal(250), NO_MIN, KG_MAX)

        assertEquals(listOf(99_500L, 99_750L).map { FixedDecimal(it) }, suggestions)
    }

    @Test
    fun givenANullBase_whenBuildingWeightSuggestions_thenTheListIsEmpty() {
        assertEquals(emptyList(), KeypadQuickValues.weightSuggestions(null, FixedDecimal(250), NO_MIN, KG_MAX))
    }

    @Test
    fun givenAPoundsBase_whenBuildingWeightSuggestionsWithA5Step_thenTheyAreTheFourStepsAroundIt() {
        val suggestions = KeypadQuickValues.weightSuggestions(FixedDecimal(18_500), FixedDecimal(500), NO_MIN, LB_MAX)

        assertEquals(listOf(17_500L, 18_000L, 19_000L, 19_500L).map { FixedDecimal(it) }, suggestions)
    }

    @Test
    fun givenTheRepsSuggestions_whenRead_thenTheyAreTheFixedBoardValues() {
        assertEquals(listOf(5L, 6L, 8L, 10L, 12L).map { FixedDecimal.ofWhole(it) }, KeypadQuickValues.repsSuggestions)
    }
}
