package com.repz.app.feature.settings.impl.presentation.profile

import kotlin.test.Test
import kotlin.test.assertEquals

class MonogramTest {
    @Test
    fun givenAnAsciiName_whenMonogrammed_thenReturnsItsUppercasedFirstLetter() {
        assertEquals("A", monogramOf("Alex"))
    }

    @Test
    fun givenALowercasePolishName_whenMonogrammed_thenReturnsTheUppercasedFirstLetter() {
        assertEquals("Ł", monogramOf("łukasz"))
    }

    @Test
    fun givenLeadingSpaces_whenMonogrammed_thenTheyAreTrimmedFirst() {
        assertEquals("A", monogramOf("  Alex"))
    }

    @Test
    fun givenANameStartingWithASurrogatePairEmoji_whenMonogrammed_thenTheFullCodePointIsKeptIntact() {
        val emojiName = "😀 Alex" // leading grinning-face emoji, a surrogate pair
        assertEquals("😀", monogramOf(emojiName))
    }

    @Test
    fun givenASingleCharacterName_whenMonogrammed_thenReturnsItUppercased() {
        assertEquals("X", monogramOf("x"))
    }

    @Test
    fun givenABlankName_whenMonogrammed_thenReturnsAnEmptyString() {
        assertEquals("", monogramOf("   "))
    }
}
