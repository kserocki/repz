package com.repz.app.core.domain.model

import kotlin.jvm.JvmInline
import kotlin.math.roundToLong

private const val GRAMS_PER_KILOGRAM = 1000.0

// The international pound is exact by definition, not a measured constant.
private const val GRAMS_PER_POUND = 453.59237

/**
 * A weight stored in whole grams (ADR-0022), so arithmetic across sets, volumes and personal
 * records never drifts the way floating-point kilograms would. Conversions round half up to the
 * nearest gram.
 */
@JvmInline
value class Weight(
    val grams: Long,
) : Comparable<Weight> {
    init {
        require(grams >= 0) { "Weight can't be negative: $grams g" }
    }

    operator fun plus(other: Weight): Weight = Weight(grams + other.grams)

    operator fun times(reps: Int): Weight {
        require(reps >= 0) { "Reps can't be negative: $reps" }
        return Weight(grams * reps)
    }

    override fun compareTo(other: Weight): Int = grams.compareTo(other.grams)

    fun inKilograms(): Double = grams / GRAMS_PER_KILOGRAM

    fun inPounds(): Double = grams / GRAMS_PER_POUND

    fun inUnit(unit: UnitSystem): Double =
        when (unit) {
            UnitSystem.KG -> inKilograms()
            UnitSystem.LB -> inPounds()
        }

    companion object {
        val ZERO: Weight = Weight(0)

        fun fromKilograms(kilograms: Double): Weight = Weight(gramsFromAmount(kilograms * GRAMS_PER_KILOGRAM))

        fun fromPounds(pounds: Double): Weight = Weight(gramsFromAmount(pounds * GRAMS_PER_POUND))

        fun fromUnit(
            amount: Double,
            unit: UnitSystem,
        ): Weight =
            when (unit) {
                UnitSystem.KG -> fromKilograms(amount)
                UnitSystem.LB -> fromPounds(amount)
            }

        private fun gramsFromAmount(amount: Double): Long {
            require(amount.isFinite()) { "Weight must be finite: $amount" }
            require(amount >= 0) { "Weight can't be negative: $amount" }
            return amount.roundToLong()
        }
    }
}
