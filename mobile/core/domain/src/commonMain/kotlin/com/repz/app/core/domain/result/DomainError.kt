package com.repz.app.core.domain.result

import com.repz.app.core.domain.model.Weight

/**
 * Marker for every typed failure a domain operation can return (no exceptions across layers).
 * Deliberately not `sealed`: Kotlin requires a sealed interface's direct subtypes to live in its
 * own package, but each feature's error type (e.g. `AuthError`) lives next to the repository it
 * belongs to. Nothing matches exhaustively over [DomainError] itself; each concrete error type
 * (like [ValidationError] or `AuthError`) stays sealed for its own exhaustive `when`.
 */
interface DomainError

/** A validation failure for a name, notes, a set weight, reps or an RPE value. */
sealed interface ValidationError : DomainError {
    data object NameBlank : ValidationError

    data class NameTooLong(
        val maxLength: Int,
    ) : ValidationError

    data class NotesTooLong(
        val maxLength: Int,
    ) : ValidationError

    data class WeightTooHeavy(
        val max: Weight,
    ) : ValidationError

    data class RepsOutOfRange(
        val range: IntRange,
    ) : ValidationError

    data object InvalidRpe : ValidationError

    data object EmailInvalid : ValidationError

    data object PasswordMissing : ValidationError

    data class PasswordTooShort(
        val minLength: Int,
    ) : ValidationError

    data class PasswordTooLong(
        val maxLength: Int,
    ) : ValidationError
}
