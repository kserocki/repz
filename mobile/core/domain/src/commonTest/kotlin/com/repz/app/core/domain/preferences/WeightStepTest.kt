package com.repz.app.core.domain.preferences

import com.repz.app.core.domain.model.UnitSystem
import kotlin.test.Test
import kotlin.test.assertEquals

class WeightStepTest {
    @Test
    fun givenKg_whenOptionsRequested_thenTheFourKgStepsAreReturnedAscending() {
        assertEquals(
            listOf(WeightStep.KG_0_5, WeightStep.KG_1_25, WeightStep.KG_2_5, WeightStep.KG_5),
            WeightStep.optionsFor(UnitSystem.KG),
        )
    }

    @Test
    fun givenLb_whenOptionsRequested_thenTheThreeLbStepsAreReturnedAscending() {
        assertEquals(
            listOf(WeightStep.LB_1, WeightStep.LB_2_5, WeightStep.LB_5),
            WeightStep.optionsFor(UnitSystem.LB),
        )
    }

    @Test
    fun givenKg_whenDefaultRequested_then2Point5KgIsReturned() {
        assertEquals(WeightStep.KG_2_5, WeightStep.defaultFor(UnitSystem.KG))
    }

    @Test
    fun givenLb_whenDefaultRequested_then5LbIsReturned() {
        assertEquals(WeightStep.LB_5, WeightStep.defaultFor(UnitSystem.LB))
    }

    @Test
    fun givenEveryStep_whenUnitSystemRead_thenItMatchesTheNamePrefix() {
        WeightStep.entries.forEach { step ->
            val expectedUnit = if (step.name.startsWith("KG")) UnitSystem.KG else UnitSystem.LB
            assertEquals(expectedUnit, step.unitSystem)
        }
    }
}
