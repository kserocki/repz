package com.repz.app.core.domain.model

import kotlin.math.roundToLong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private const val MAX_POUND_HUNDREDTHS = 220_462
private const val MAX_GRAMS = 1_000_000

class WeightTest {
    @Test
    fun givenKilogramValues_whenConvertedToGramsAndBack_thenTheyAreExactToThreeDecimals() {
        val kilograms = listOf(0.0, 1.0, 82.5, 61.25, 103.125, 1000.0)

        kilograms.forEach { value ->
            val weight = Weight.fromKilograms(value)

            assertEquals((value * 1000).roundToLong(), weight.grams)
            assertEquals(value, weight.inKilograms())
        }
    }

    @Test
    fun givenPounds_whenConvertedFromThePoundsValue_thenGramsRoundHalfUp() {
        assertEquals(61_235L, Weight.fromPounds(135.0).grams)
    }

    @Test
    fun givenAHalfGramAmount_whenConverted_thenRoundsUp() {
        // 0.0625 kg * 1000 = 62.5 g exactly: the tie must round up, not down or to even.
        assertEquals(63L, Weight.fromKilograms(0.0625).grams)
    }

    @Test
    fun givenPoundValuesWithTwoDecimals_whenRoundTripped_thenTheyRecoverExactly() {
        for (hundredths in 0..MAX_POUND_HUNDREDTHS) {
            val pounds = hundredths / 100.0

            val roundTripped = (Weight.fromPounds(pounds).inPounds() * 100).roundToLong()

            assertEquals(hundredths.toLong(), roundTripped)
        }
    }

    @Test
    fun givenKilogramValuesWithThreeDecimals_whenConverted_thenGramsAreExact() {
        for (grams in 0..MAX_GRAMS) {
            val kilograms = grams / 1000.0

            assertEquals(grams.toLong(), Weight.fromKilograms(kilograms).grams)
        }
    }

    @Test
    fun givenTwoWeights_whenAdded_thenGramsSum() {
        assertEquals(Weight(1500), Weight(1000) + Weight(500))
    }

    @Test
    fun givenAWeightAndReps_whenMultiplied_thenGramsScale() {
        assertEquals(Weight(2500), Weight(500) * 5)
    }

    @Test
    fun givenNegativeReps_whenMultiplied_thenRequireFails() {
        assertFailsWith<IllegalArgumentException> { Weight(500) * -1 }
    }

    @Test
    fun givenTwoWeights_whenCompared_thenOrderIsByGrams() {
        assertTrue(Weight(500) < Weight(1000))
        assertTrue(Weight(1000) > Weight(500))
        assertEquals(0, Weight(1000).compareTo(Weight(1000)))
    }

    @Test
    fun givenANegativeAmount_whenConstructed_thenRequireFails() {
        assertFailsWith<IllegalArgumentException> { Weight(-1) }
    }

    @Test
    fun givenANegativeAmount_whenConvertedFromKilograms_thenRequireFails() {
        assertFailsWith<IllegalArgumentException> { Weight.fromKilograms(-1.0) }
    }

    @Test
    fun givenNaN_whenConvertedFromKilograms_thenRequireFails() {
        assertFailsWith<IllegalArgumentException> { Weight.fromKilograms(Double.NaN) }
    }

    @Test
    fun givenInfinity_whenConvertedFromKilograms_thenRequireFails() {
        assertFailsWith<IllegalArgumentException> { Weight.fromKilograms(Double.POSITIVE_INFINITY) }
    }
}
