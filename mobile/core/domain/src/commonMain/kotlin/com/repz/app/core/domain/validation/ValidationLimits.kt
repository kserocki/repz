package com.repz.app.core.domain.validation

import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The server's limits (ADR-0020), plus the validators shared by two or more features. Validators
 * for a single feature's own fields (template target reps range, rest) live with that feature.
 */
object ValidationLimits {
    const val NAME_MAX_LENGTH = 100
    const val NOTES_MAX_LENGTH = 2000
    const val EXERCISES_PER_WORKOUT_MAX = 50
    const val SETS_PER_EXERCISE_MAX = 50
    const val EXERCISES_PER_TEMPLATE_MAX = 50
    val TARGET_SETS_RANGE: IntRange = 1..20
    val REPS_RANGE: IntRange = 0..999
    val REST_MAX: Duration = 3600.seconds
    val SET_WEIGHT_MAX: Weight = Weight(1_000_000)

    /** Trims [raw] and validates the result; the trimmed name is the success value. */
    fun validateName(raw: String): DomainResult<String, ValidationError> {
        val trimmed = raw.trim()
        return when {
            trimmed.isEmpty() -> DomainResult.Failure(ValidationError.NameBlank)
            trimmed.length > NAME_MAX_LENGTH -> DomainResult.Failure(ValidationError.NameTooLong(NAME_MAX_LENGTH))
            else -> DomainResult.Success(trimmed)
        }
    }

    fun validateNotes(raw: String): DomainResult<String, ValidationError.NotesTooLong> =
        if (raw.length > NOTES_MAX_LENGTH) {
            DomainResult.Failure(ValidationError.NotesTooLong(NOTES_MAX_LENGTH))
        } else {
            DomainResult.Success(raw)
        }

    fun validateSetWeight(weight: Weight): DomainResult<Weight, ValidationError.WeightTooHeavy> =
        if (weight > SET_WEIGHT_MAX) {
            DomainResult.Failure(ValidationError.WeightTooHeavy(SET_WEIGHT_MAX))
        } else {
            DomainResult.Success(weight)
        }

    fun validateReps(reps: Int): DomainResult<Int, ValidationError.RepsOutOfRange> =
        if (reps in REPS_RANGE) DomainResult.Success(reps) else DomainResult.Failure(ValidationError.RepsOutOfRange(REPS_RANGE))
}
