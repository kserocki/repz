package com.repz.app.core.domain.model.account

import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PasswordTest {
    @Test
    fun givenASevenCharacterPassword_whenValidatedForRegistration_thenFailsWithPasswordTooShort() {
        assertEquals(
            DomainResult.Failure(ValidationError.PasswordTooShort(8)),
            Password.forRegistration("a".repeat(7)),
        )
    }

    @Test
    fun givenAnEightCharacterPassword_whenValidatedForRegistration_thenItSucceeds() {
        val raw = "a".repeat(8)

        val success = assertIs<DomainResult.Success<Password>>(Password.forRegistration(raw))
        assertEquals(raw, success.value.value)
    }

    @Test
    fun givenAOneHundredAndTwentyEightCharacterPassword_whenValidatedForRegistration_thenItSucceeds() {
        val raw = "a".repeat(128)

        val success = assertIs<DomainResult.Success<Password>>(Password.forRegistration(raw))
        assertEquals(raw, success.value.value)
    }

    @Test
    fun givenAOneHundredAndTwentyNineCharacterPassword_whenValidatedForRegistration_thenFailsWithPasswordTooLong() {
        assertEquals(
            DomainResult.Failure(ValidationError.PasswordTooLong(128)),
            Password.forRegistration("a".repeat(129)),
        )
    }

    @Test
    fun givenAnEmptyPassword_whenValidatedForRegistration_thenFailsWithPasswordTooShort() {
        assertEquals(DomainResult.Failure(ValidationError.PasswordTooShort(8)), Password.forRegistration(""))
    }

    @Test
    fun givenAnEightSpacePassword_whenValidatedForRegistration_thenItSucceedsUntrimmed() {
        val raw = " ".repeat(8)

        val success = assertIs<DomainResult.Success<Password>>(Password.forRegistration(raw))
        assertEquals(raw, success.value.value)
    }

    @Test
    fun givenAPasswordWithLeadingAndTrailingSpaces_whenValidatedForRegistration_thenTheSpacesAreKept() {
        val raw = "  secret1  "

        val success = assertIs<DomainResult.Success<Password>>(Password.forRegistration(raw))
        assertEquals(raw, success.value.value)
    }

    @Test
    fun givenAnEmptyPassword_whenValidatedForLogIn_thenFailsWithPasswordMissing() {
        assertEquals(DomainResult.Failure(ValidationError.PasswordMissing), Password.forLogIn(""))
    }

    @Test
    fun givenAOneCharacterPassword_whenValidatedForLogIn_thenItSucceeds() {
        val success = assertIs<DomainResult.Success<Password>>(Password.forLogIn("a"))
        assertEquals("a", success.value.value)
    }

    @Test
    fun givenASingleSpacePassword_whenValidatedForLogIn_thenItSucceeds() {
        val success = assertIs<DomainResult.Success<Password>>(Password.forLogIn(" "))
        assertEquals(" ", success.value.value)
    }

    @Test
    fun givenAOneHundredAndTwentyEightCharacterPassword_whenValidatedForLogIn_thenItSucceeds() {
        val raw = "a".repeat(128)

        val success = assertIs<DomainResult.Success<Password>>(Password.forLogIn(raw))
        assertEquals(raw, success.value.value)
    }

    @Test
    fun givenAOneHundredAndTwentyNineCharacterPassword_whenValidatedForLogIn_thenFailsWithPasswordTooLong() {
        assertEquals(
            DomainResult.Failure(ValidationError.PasswordTooLong(128)),
            Password.forLogIn("a".repeat(129)),
        )
    }

    @Test
    fun givenAPasswordWithAUnicodeCharacter_whenValidatedForRegistration_thenItSucceeds() {
        val raw = "pässwörd1"

        val success = assertIs<DomainResult.Success<Password>>(Password.forRegistration(raw))
        assertEquals(raw, success.value.value)
    }

    @Test
    fun givenAPassword_whenToStringIsCalled_thenTheValueIsMasked() {
        val password = assertIs<DomainResult.Success<Password>>(Password.forRegistration("supersecret")).value

        assertEquals("Password(***)", password.toString())
    }
}
