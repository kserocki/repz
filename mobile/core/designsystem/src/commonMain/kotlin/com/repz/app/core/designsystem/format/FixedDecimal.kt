package com.repz.app.core.designsystem.format

import androidx.compose.runtime.Immutable
import kotlin.jvm.JvmInline

private const val SINGLE_FRACTION_DIGIT_DIVISOR = 10L

/**
 * A non-negative decimal with exactly two fraction digits, stored as hundredths so weight, reps
 * and RPE display values never drift the way floating-point arithmetic would: 82.5 =
 * `FixedDecimal(8250)` (ADR-0028). It knows nothing about units, locales or domain types; those
 * live at the `core:ui` boundary ([com.repz.app.core.ui.format.WeightFormat]).
 */
@Immutable
@JvmInline
value class FixedDecimal(
    val hundredths: Long,
) : Comparable<FixedDecimal> {
    init {
        require(hundredths >= 0) { "FixedDecimal can't be negative: $hundredths hundredths" }
    }

    operator fun plus(other: FixedDecimal): FixedDecimal = FixedDecimal(hundredths + other.hundredths)

    /** The keypad never lets a value go below its field's minimum, so a negative result here is a programming error. */
    operator fun minus(other: FixedDecimal): FixedDecimal = FixedDecimal(hundredths - other.hundredths)

    override fun compareTo(other: FixedDecimal): Int = hundredths.compareTo(other.hundredths)

    /** Rounds half up to the nearest multiple of [step], e.g. 81.25 rounded to a 2.5 step is 82.5. */
    fun roundedToMultipleOf(step: FixedDecimal): FixedDecimal {
        require(step.hundredths > 0) { "step must be positive: ${step.hundredths} hundredths" }
        val stepHundredths = step.hundredths
        val multiples = (hundredths * 2 + stepHundredths) / (2 * stepHundredths)
        return FixedDecimal(multiples * stepHundredths)
    }

    /** No grouping, trailing zeros trimmed: `FixedDecimal(8250).format(...)` is "82.5" or "82,5". */
    fun format(symbols: RepzNumberSymbols): String {
        val whole = hundredths / HUNDREDTHS_PER_UNIT
        val fraction = hundredths % HUNDREDTHS_PER_UNIT
        return when {
            fraction == 0L -> whole.toString()
            fraction % SINGLE_FRACTION_DIGIT_DIVISOR == 0L -> "$whole${symbols.decimalSeparator}${fraction / SINGLE_FRACTION_DIGIT_DIVISOR}"
            else -> "$whole${symbols.decimalSeparator}${fraction.toString().padStart(2, '0')}"
        }
    }

    companion object {
        val ZERO: FixedDecimal = FixedDecimal(0)

        /** How many hundredths make one whole unit (2 fraction digits); shared with [KeypadRules]
         * and the display row instead of each repeating its own copy. */
        const val HUNDREDTHS_PER_UNIT = 100L

        fun ofWhole(value: Long): FixedDecimal = FixedDecimal(value * HUNDREDTHS_PER_UNIT)
    }
}
