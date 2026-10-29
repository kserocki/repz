package com.repz.buildlogic.android

import kotlin.test.Test
import kotlin.test.assertEquals

class GenerateAndroidAppNameResourcesTaskTest {

    @Test
    fun givenTheDefaultLocaleTag_whenAndroidValuesDirectoryNameIsCalled_thenItMapsToValues() {
        assertEquals("values", androidValuesDirectoryName("en"))
    }

    @Test
    fun givenALanguageOnlyTag_whenAndroidValuesDirectoryNameIsCalled_thenItMapsToValuesLanguage() {
        assertEquals("values-pl", androidValuesDirectoryName("pl"))
    }

    @Test
    fun givenALanguageRegionTag_whenAndroidValuesDirectoryNameIsCalled_thenRegionGetsAnRPrefix() {
        assertEquals("values-pt-rBR", androidValuesDirectoryName("pt-BR"))
    }

    @Test
    fun givenAnAmpersand_whenEscapeAndroidStringIsCalled_thenItIsEscaped() {
        assertEquals("Fish &amp; Chips", escapeAndroidString("Fish & Chips"))
    }

    @Test
    fun givenALessThanSign_whenEscapeAndroidStringIsCalled_thenItIsEscaped() {
        assertEquals("1 &lt; 2", escapeAndroidString("1 < 2"))
    }

    @Test
    fun givenADoubleQuote_whenEscapeAndroidStringIsCalled_thenItIsBackslashEscaped() {
        assertEquals("Say \\\"hi\\\"", escapeAndroidString("Say \"hi\""))
    }

    @Test
    fun givenAnApostrophe_whenEscapeAndroidStringIsCalled_thenItIsBackslashEscaped() {
        assertEquals("Repz\\'s", escapeAndroidString("Repz's"))
    }

    @Test
    fun givenAnAtSign_whenEscapeAndroidStringIsCalled_thenItIsBackslashEscaped() {
        assertEquals("user\\@repz", escapeAndroidString("user@repz"))
    }

    @Test
    fun givenAQuestionMark_whenEscapeAndroidStringIsCalled_thenItIsBackslashEscaped() {
        assertEquals("Repz\\?", escapeAndroidString("Repz?"))
    }

    @Test
    fun givenAnAtSignInTheMiddleOfAName_whenEscapeAndroidStringIsCalled_thenTheNameStaysValid() {
        assertEquals("contact\\@repz.app", escapeAndroidString("contact@repz.app"))
    }

    @Test
    fun givenAQuestionMarkInTheMiddleOfAName_whenEscapeAndroidStringIsCalled_thenTheNameStaysValid() {
        assertEquals("what\\?is this", escapeAndroidString("what?is this"))
    }
}
