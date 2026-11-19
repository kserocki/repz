package com.repz.app.core.ui.format

import com.repz.app.core.designsystem.format.FixedDecimal
import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.validation.ValidationLimits
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val KG_ROUND_TRIP_UPPER_BOUND_HUNDREDTHS = 100_000L
private const val LB_ROUND_TRIP_UPPER_BOUND_HUNDREDTHS = 220_462L

class WeightFormatTest {
    @Test
    fun given82501Grams_whenConvertedToKgDisplay_thenItRoundsDownToEighty2Point50() {
        assertEquals(FixedDecimal(8250), WeightFormat.toDisplay(Weight(82_501), UnitSystem.KG))
    }

    @Test
    fun given82505Grams_whenConvertedToKgDisplay_thenTheExactHalfRoundsUpToEighty2Point51() {
        assertEquals(FixedDecimal(8251), WeightFormat.toDisplay(Weight(82_505), UnitSystem.KG))
    }

    @Test
    fun given61250Grams_whenConvertedToKgDisplay_thenItRoundsDownToSixty1Point25() {
        assertEquals(FixedDecimal(6125), WeightFormat.toDisplay(Weight(61_250), UnitSystem.KG))
    }

    @Test
    fun given83915Grams_whenConvertedToLbDisplay_then185Point00IsReturned() {
        assertEquals(FixedDecimal.ofWhole(185), WeightFormat.toDisplay(Weight(83_915), UnitSystem.LB))
    }

    @Test
    fun givenKg_whenMaxSetWeightRequested_then1000IsReturnedAndRoundTripsAroundTheLimit() {
        val max = WeightFormat.maxSetWeight(UnitSystem.KG)

        assertEquals(FixedDecimal.ofWhole(1000), max)
        assertTrue(WeightFormat.fromDisplay(max, UnitSystem.KG) <= ValidationLimits.SET_WEIGHT_MAX)
        assertTrue(WeightFormat.fromDisplay(max + CENT, UnitSystem.KG) > ValidationLimits.SET_WEIGHT_MAX)
    }

    @Test
    fun givenLb_whenMaxSetWeightRequested_then2204Point62IsReturnedAndRoundTripsAroundTheLimit() {
        val max = WeightFormat.maxSetWeight(UnitSystem.LB)

        assertEquals(FixedDecimal(220_462), max)
        assertTrue(WeightFormat.fromDisplay(max, UnitSystem.LB) <= ValidationLimits.SET_WEIGHT_MAX)
        assertTrue(WeightFormat.fromDisplay(max + CENT, UnitSystem.LB) > ValidationLimits.SET_WEIGHT_MAX)
    }

    @Test
    fun givenEveryKgDisplayHundredthUpToTheMax_whenConvertedToWeightAndBack_thenItRoundTrips() {
        for (hundredths in 0..KG_ROUND_TRIP_UPPER_BOUND_HUNDREDTHS) {
            val display = FixedDecimal(hundredths)

            val roundTripped = WeightFormat.toDisplay(WeightFormat.fromDisplay(display, UnitSystem.KG), UnitSystem.KG)

            assertEquals(display, roundTripped, "kg round trip failed for $hundredths hundredths")
        }
    }

    @Test
    fun givenEveryLbDisplayHundredthUpToTheMax_whenConvertedToWeightAndBack_thenItRoundTrips() {
        for (hundredths in 0..LB_ROUND_TRIP_UPPER_BOUND_HUNDREDTHS) {
            val display = FixedDecimal(hundredths)

            val roundTripped = WeightFormat.toDisplay(WeightFormat.fromDisplay(display, UnitSystem.LB), UnitSystem.LB)

            assertEquals(display, roundTripped, "lb round trip failed for $hundredths hundredths")
        }
    }

    private companion object {
        val CENT = FixedDecimal(1)
    }
}
