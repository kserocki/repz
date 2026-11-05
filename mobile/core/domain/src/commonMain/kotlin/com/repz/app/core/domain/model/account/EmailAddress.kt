package com.repz.app.core.domain.model.account

import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import kotlin.jvm.JvmInline

private val EMAIL_SHAPE = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

/**
 * A syntactically valid email address, trimmed. The shape check (one '@', a non-empty local part,
 * a dotted domain, no whitespace) is deliberately looser than the server's `z.email()`, so the
 * client never rejects something the API would accept; the server has the final say.
 */
@JvmInline
value class EmailAddress private constructor(
    val value: String,
) {
    companion object {
        const val MAX_LENGTH = 254

        fun of(raw: String): DomainResult<EmailAddress, ValidationError.EmailInvalid> {
            val trimmed = raw.trim()
            val isValid = trimmed.length in 1..MAX_LENGTH && EMAIL_SHAPE.matches(trimmed)
            return if (isValid) {
                DomainResult.Success(EmailAddress(trimmed))
            } else {
                DomainResult.Failure(ValidationError.EmailInvalid)
            }
        }
    }
}
