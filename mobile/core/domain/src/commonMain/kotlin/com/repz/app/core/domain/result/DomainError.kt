package com.repz.app.core.domain.result

import com.repz.app.core.domain.model.Weight

/** Marker for every typed failure a domain operation can return (no exceptions across layers). */
sealed interface DomainError

/**
 * Only the validation failures this issue's code returns. Other error families (not found,
 * unauthorized, network, sync) are added by the first issue that needs them.
 */
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
}
