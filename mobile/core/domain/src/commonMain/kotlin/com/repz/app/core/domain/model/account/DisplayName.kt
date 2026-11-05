package com.repz.app.core.domain.model.account

import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import kotlin.jvm.JvmInline

/** Trimmed, 1-50 characters, matching the server's rule. */
@JvmInline
value class DisplayName private constructor(
    val value: String,
) {
    companion object {
        const val MAX_LENGTH = 50

        fun of(raw: String): DomainResult<DisplayName, ValidationError> {
            val trimmed = raw.trim()
            return when {
                trimmed.isEmpty() -> DomainResult.Failure(ValidationError.NameBlank)
                trimmed.length > MAX_LENGTH -> DomainResult.Failure(ValidationError.NameTooLong(MAX_LENGTH))
                else -> DomainResult.Success(DisplayName(trimmed))
            }
        }
    }
}
