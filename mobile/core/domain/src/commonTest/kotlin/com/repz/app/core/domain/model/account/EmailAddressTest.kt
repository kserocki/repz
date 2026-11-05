package com.repz.app.core.domain.model.account

import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class EmailAddressTest {
    @Test
    fun givenAPlainEmail_whenParsed_thenItSucceeds() {
        val result = EmailAddress.of("alex@example.com")

        val success = assertIs<DomainResult.Success<EmailAddress>>(result)
        assertEquals("alex@example.com", success.value.value)
    }

    @Test
    fun givenAnEmailWithSurroundingWhitespace_whenParsed_thenItIsTrimmed() {
        val result = EmailAddress.of("  alex@example.com  ")

        val success = assertIs<DomainResult.Success<EmailAddress>>(result)
        assertEquals("alex@example.com", success.value.value)
    }

    @Test
    fun givenAStringWithoutAnAtSign_whenParsed_thenFailsWithEmailInvalid() {
        assertEquals(DomainResult.Failure(ValidationError.EmailInvalid), EmailAddress.of("not-an-email"))
    }

    @Test
    fun givenAnEmailWithoutADottedDomain_whenParsed_thenFailsWithEmailInvalid() {
        assertEquals(DomainResult.Failure(ValidationError.EmailInvalid), EmailAddress.of("alex@example"))
    }

    @Test
    fun givenAnEmailWithTwoAtSigns_whenParsed_thenFailsWithEmailInvalid() {
        assertEquals(DomainResult.Failure(ValidationError.EmailInvalid), EmailAddress.of("al@ex@example.com"))
    }

    @Test
    fun givenAnEmailWithInternalWhitespace_whenParsed_thenFailsWithEmailInvalid() {
        assertEquals(DomainResult.Failure(ValidationError.EmailInvalid), EmailAddress.of("alex smith@example.com"))
    }

    @Test
    fun givenAWhitespaceOnlyString_whenParsed_thenFailsWithEmailInvalid() {
        assertEquals(DomainResult.Failure(ValidationError.EmailInvalid), EmailAddress.of("   "))
    }

    @Test
    fun givenAnEmptyString_whenParsed_thenFailsWithEmailInvalid() {
        assertEquals(DomainResult.Failure(ValidationError.EmailInvalid), EmailAddress.of(""))
    }

    @Test
    fun givenAnEmailWithAUnicodeLocalPart_whenParsed_thenItSucceeds() {
        val result = EmailAddress.of("józef@example.com")

        val success = assertIs<DomainResult.Success<EmailAddress>>(result)
        assertEquals("józef@example.com", success.value.value)
    }

    @Test
    fun givenAnEmailExactlyAtTheMaxLength_whenParsed_thenItSucceeds() {
        val email = emailOfLength(EmailAddress.MAX_LENGTH)

        val result = EmailAddress.of(email)

        val success = assertIs<DomainResult.Success<EmailAddress>>(result)
        assertEquals(email, success.value.value)
    }

    @Test
    fun givenAnEmailOneOverTheMaxLength_whenParsed_thenFailsWithEmailInvalid() {
        val email = emailOfLength(EmailAddress.MAX_LENGTH + 1)

        assertEquals(DomainResult.Failure(ValidationError.EmailInvalid), EmailAddress.of(email))
    }

    /** Builds a syntactically valid `local@domain.com` of exactly [length] characters. */
    private fun emailOfLength(length: Int): String {
        val suffix = "@a.com"
        val localLength = length - suffix.length
        return "a".repeat(localLength) + suffix
    }
}
