package com.repz.app.core.domain.preferences

import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.model.UnitSystem.KG
import com.repz.app.core.domain.model.UnitSystem.LB

private const val HALF_KG = 0.5
private const val ONE_AND_QUARTER_KG = 1.25
private const val TWO_AND_HALF_KG = 2.5
private const val FIVE_KG = 5.0
private const val ONE_LB = 1.0
private const val TWO_AND_HALF_LB = 2.5
private const val FIVE_LB = 5.0

/** The amount the number keypad's +/- buttons step a set's weight by, per [unitSystem]. */
enum class WeightStep(
    val unitSystem: UnitSystem,
    val amountInUnit: Double,
) {
    KG_0_5(KG, HALF_KG),
    KG_1_25(KG, ONE_AND_QUARTER_KG),
    KG_2_5(KG, TWO_AND_HALF_KG),
    KG_5(KG, FIVE_KG),
    LB_1(LB, ONE_LB),
    LB_2_5(LB, TWO_AND_HALF_LB),
    LB_5(LB, FIVE_LB),
    ;

    companion object {
        /** The allowed steps for [unitSystem], ascending; what the keypad's Step chip cycles through. */
        fun optionsFor(unitSystem: UnitSystem): List<WeightStep> = entries.filter { it.unitSystem == unitSystem }

        /** The smallest common plate pair per unit: 2 x 1.25 kg, 2 x 2.5 lb. */
        fun defaultFor(unitSystem: UnitSystem): WeightStep =
            when (unitSystem) {
                KG -> KG_2_5
                LB -> LB_5
            }
    }
}
