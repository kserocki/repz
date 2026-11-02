package com.repz.app.core.domain.model

import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import kotlin.jvm.JvmInline
import kotlin.math.floor

private const val MIN_DOUBLED_RPE = 12
private const val MAX_DOUBLED_RPE = 20

/**
 * A rate of perceived exertion, 6.0 to 10.0 in steps of 0.5. The only way to obtain one is [of],
 * so an invalid value is unrepresentable. Halves are exact in binary floating point, so `==` on
 * [value] is safe.
 */
@JvmInline
value class Rpe private constructor(
    val value: Double,
) {
    companion object {
        /** The 9 valid values, ascending, for the keypad's RPE chips. */
        val ALL: List<Rpe> = (MIN_DOUBLED_RPE..MAX_DOUBLED_RPE).map { Rpe(it / 2.0) }

        fun of(value: Double): DomainResult<Rpe, ValidationError.InvalidRpe> {
            val doubled = value * 2
            val isValidStep =
                value.isFinite() && doubled == floor(doubled) && doubled in MIN_DOUBLED_RPE.toDouble()..MAX_DOUBLED_RPE.toDouble()
            return if (isValidStep) DomainResult.Success(Rpe(value)) else DomainResult.Failure(ValidationError.InvalidRpe)
        }
    }
}
