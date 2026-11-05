package com.repz.app.core.domain.model.account

import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DisplayNameTest {
    @Test
    fun givenAnEmptyName_whenParsed_thenFailsWithNameBlank() {
        assertEquals(DomainResult.Failure(ValidationError.NameBlank), DisplayName.of(""))
    }

    @Test
    fun givenAWhitespaceOnlyName_whenParsed_thenFailsWithNameBlank() {
        assertEquals(DomainResult.Failure(ValidationError.NameBlank), DisplayName.of("   "))
    }

    @Test
    fun givenAOneCharacterName_whenParsed_thenItSucceeds() {
        val success = assertIs<DomainResult.Success<DisplayName>>(DisplayName.of("A"))
        assertEquals("A", success.value.value)
    }

    @Test
    fun givenANameWithSurroundingWhitespace_whenParsed_thenItIsTrimmed() {
        val success = assertIs<DomainResult.Success<DisplayName>>(DisplayName.of("  Alex  "))
        assertEquals("Alex", success.value.value)
    }

    @Test
    fun givenAFiftyCharacterName_whenParsed_thenItSucceeds() {
        val name = "a".repeat(50)

        val success = assertIs<DomainResult.Success<DisplayName>>(DisplayName.of(name))
        assertEquals(name, success.value.value)
    }

    @Test
    fun givenAFiftyOneCharacterName_whenParsed_thenFailsWithNameTooLong() {
        assertEquals(DomainResult.Failure(ValidationError.NameTooLong(50)), DisplayName.of("a".repeat(51)))
    }

    @Test
    fun givenAFiftyCharacterNameWithSurroundingWhitespace_whenParsed_thenItSucceedsTrimmed() {
        val name = "a".repeat(50)

        val success = assertIs<DomainResult.Success<DisplayName>>(DisplayName.of("  $name  "))
        assertEquals(name, success.value.value)
    }

    @Test
    fun givenANameWithAUnicodeCharacter_whenParsed_thenItSucceeds() {
        val success = assertIs<DomainResult.Success<DisplayName>>(DisplayName.of("Józef"))
        assertEquals("Józef", success.value.value)
    }
}
