package com.repz.app.core.ui.format

import com.repz.app.core.designsystem.format.FixedDecimal
import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.validation.ValidationLimits

private const val GRAMS_PER_KG_DISPLAY_DIVISOR = 10L
private const val POUND_HUNDREDTHS_NUMERATOR_SCALE = 10_000_000L
private const val HUNDREDTHS_PER_UNIT = 100.0

// The international pound is exact by definition: 1 lb = 453.59237 g, i.e. 45 359 237 / 100 000 g.
private const val GRAMS_PER_POUND_SCALED = 45_359_237L

/**
 * Converts a [Weight] (whole grams) to and from the keypad's two-decimal display [FixedDecimal]
 * (ADR-0028), with exact integer maths so the conversion never drifts: a gram is exactly a tenth
 * of a display hundredth of a kg, so `toDisplay` for kg is `(g + 5) / 10`; a pound has no exact
 * decimal gram equivalent, so its numerator is scaled up by `10^7` before the same half-up integer
 * division by the exact grams-per-pound ratio.
 */
object WeightFormat {
    fun toDisplay(
        weight: Weight,
        unit: UnitSystem,
    ): FixedDecimal =
        FixedDecimal(
            when (unit) {
                UnitSystem.KG -> roundHalfUp(weight.grams, GRAMS_PER_KG_DISPLAY_DIVISOR)
                UnitSystem.LB -> roundHalfUp(weight.grams * POUND_HUNDREDTHS_NUMERATOR_SCALE, GRAMS_PER_POUND_SCALED)
            },
        )

    /** The one conversion point from a display amount back to a domain [Weight] (ADR-0022). */
    fun fromDisplay(
        amount: FixedDecimal,
        unit: UnitSystem,
    ): Weight = Weight.fromUnit(amount.hundredths / HUNDREDTHS_PER_UNIT, unit)

    /** The largest display value whose [fromDisplay] is still `<= ValidationLimits.SET_WEIGHT_MAX`. */
    fun maxSetWeight(unit: UnitSystem): FixedDecimal = toDisplay(ValidationLimits.SET_WEIGHT_MAX, unit)

    /** `numerator / denominator` rounded to the nearest integer, ties rounding up; both must be non-negative. */
    private fun roundHalfUp(
        numerator: Long,
        denominator: Long,
    ): Long = (2 * numerator + denominator) / (2 * denominator)
}
