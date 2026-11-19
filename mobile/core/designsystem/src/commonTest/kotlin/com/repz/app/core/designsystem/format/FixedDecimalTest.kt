package com.repz.app.core.designsystem.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private val PL_SYMBOLS = RepzNumberSymbols(',')

class FixedDecimalTest {
    @Test
    fun givenWholeAndFractionalHundredths_whenFormattedWithTheDotSeparator_thenTrailingZerosAreTrimmedAndThereIsNoGrouping() {
        assertEquals("0", FixedDecimal(0).format(RepzNumberSymbols.Default))
        assertEquals("82.5", FixedDecimal(8_250).format(RepzNumberSymbols.Default))
        assertEquals("61.25", FixedDecimal(6_125).format(RepzNumberSymbols.Default))
        assertEquals("135", FixedDecimal(13_500).format(RepzNumberSymbols.Default))
        assertEquals("1000", FixedDecimal(100_000).format(RepzNumberSymbols.Default))
        assertEquals("2204.62", FixedDecimal(220_462).format(RepzNumberSymbols.Default))
    }

    @Test
    fun givenTheSameHundredths_whenFormattedWithThePolishComma_thenOnlyTheSeparatorChanges() {
        assertEquals("0", FixedDecimal(0).format(PL_SYMBOLS))
        assertEquals("82,5", FixedDecimal(8_250).format(PL_SYMBOLS))
        assertEquals("61,25", FixedDecimal(6_125).format(PL_SYMBOLS))
        assertEquals("135", FixedDecimal(13_500).format(PL_SYMBOLS))
        assertEquals("1000", FixedDecimal(100_000).format(PL_SYMBOLS))
        assertEquals("2204,62", FixedDecimal(220_462).format(PL_SYMBOLS))
    }

    @Test
    fun givenAnExactMultiple_whenRoundedToAStep_thenItIsUnchanged() {
        assertEquals(FixedDecimal(8_250), FixedDecimal(8_250).roundedToMultipleOf(FixedDecimal(250)))
        assertEquals(FixedDecimal(8_000), FixedDecimal(8_000).roundedToMultipleOf(FixedDecimal(125)))
        assertEquals(FixedDecimal(5_000), FixedDecimal(5_000).roundedToMultipleOf(FixedDecimal(50)))
    }

    @Test
    fun givenAHalfwayValue_whenRoundedToA2Dot5Step_thenTheTieRoundsUp() {
        // 81.25 is exactly halfway between 80 and 82.5.
        assertEquals(FixedDecimal(8_250), FixedDecimal(8_125).roundedToMultipleOf(FixedDecimal(250)))
    }

    @Test
    fun givenAHalfwayValue_whenRoundedToA0Dot5Step_thenTheTieRoundsUp() {
        // 81.25 is exactly halfway between 81.0 and 81.5.
        assertEquals(FixedDecimal(8_150), FixedDecimal(8_125).roundedToMultipleOf(FixedDecimal(50)))
    }

    @Test
    fun givenAnOffGridValue_whenRoundedToA1Dot25Step_thenItRoundsToTheNearestMultiple() {
        // 81.80 is closer to 81.25 than to 82.50.
        assertEquals(FixedDecimal(8_125), FixedDecimal(8_180).roundedToMultipleOf(FixedDecimal(125)))
    }

    @Test
    fun givenTwoFixedDecimals_whenAdded_thenHundredthsSum() {
        assertEquals(FixedDecimal(300), FixedDecimal(100) + FixedDecimal(200))
    }

    @Test
    fun givenAMinuendSmallerThanTheSubtrahend_whenSubtracted_thenRequireFails() {
        assertFailsWith<IllegalArgumentException> { FixedDecimal(100) - FixedDecimal(200) }
    }

    @Test
    fun givenANegativeHundredths_whenConstructed_thenRequireFails() {
        assertFailsWith<IllegalArgumentException> { FixedDecimal(-1) }
    }

    @Test
    fun givenTwoFixedDecimals_whenCompared_thenOrderIsByHundredths() {
        assertTrue(FixedDecimal(100) < FixedDecimal(200))
        assertTrue(FixedDecimal(200) > FixedDecimal(100))
        assertEquals(0, FixedDecimal(100).compareTo(FixedDecimal(100)))
    }

    @Test
    fun givenAWholeNumber_whenBuiltWithOfWhole_thenItHasNoFraction() {
        assertEquals(FixedDecimal(500), FixedDecimal.ofWhole(5))
        assertEquals(FixedDecimal.ZERO, FixedDecimal.ofWhole(0))
    }
}
