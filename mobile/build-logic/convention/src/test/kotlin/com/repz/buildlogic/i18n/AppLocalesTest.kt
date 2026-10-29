package com.repz.buildlogic.i18n

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.gradle.api.GradleException

class AppLocalesTest {

    private lateinit var composeResourcesDirectory: File

    @BeforeTest
    fun setUp() {
        composeResourcesDirectory = createTempDirectory("app-locales-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        composeResourcesDirectory.deleteRecursively()
    }

    @Test
    fun givenOnlyTheDefaultValuesFolder_whenReadAppLocalesIsCalled_thenItMapsToEn() {
        writeStrings("values", "Repz")

        val locales = readAppLocales(composeResourcesDirectory)

        assertEquals(
            listOf(AppLocale(tag = "en", valuesDirectoryName = "values", appName = "Repz")),
            locales,
        )
    }

    @Test
    fun givenAValuesPlFolder_whenReadAppLocalesIsCalled_thenItMapsToPl() {
        writeStrings("values", "Repz")
        writeStrings("values-pl", "Repz")

        val locales = readAppLocales(composeResourcesDirectory)

        assertEquals(listOf("en", "pl"), locales.map { it.tag })
        assertEquals("values-pl", locales.single { it.tag == "pl" }.valuesDirectoryName)
    }

    @Test
    fun givenAValuesPtRBrFolder_whenReadAppLocalesIsCalled_thenItMapsToPtBr() {
        writeStrings("values", "Repz")
        writeStrings("values-pt-rBR", "Repz")

        val locales = readAppLocales(composeResourcesDirectory)

        assertEquals("pt-BR", locales.single { it.valuesDirectoryName == "values-pt-rBR" }.tag)
    }

    @Test
    fun givenAValuesDarkFolder_whenReadAppLocalesIsCalled_thenItIsIgnored() {
        writeStrings("values", "Repz")
        File(composeResourcesDirectory, "values-dark").mkdirs()

        val locales = readAppLocales(composeResourcesDirectory)

        assertEquals(
            listOf(AppLocale(tag = "en", valuesDirectoryName = "values", appName = "Repz")),
            locales,
        )
    }

    @Test
    fun givenAValuesPlDarkFolderAlongsideValuesPl_whenReadAppLocalesIsCalled_thenThereIsNoDuplicate() {
        writeStrings("values", "Repz")
        writeStrings("values-pl", "Repz")
        writeStrings("values-pl-dark", "Repz")

        val locales = readAppLocales(composeResourcesDirectory)

        assertEquals(listOf("en", "pl"), locales.map { it.tag })
    }

    @Test
    fun givenAnUnsupportedQualifier_whenReadAppLocalesIsCalled_thenItFails() {
        writeStrings("values-xx1", "Repz")

        assertFailsWith<GradleException> { readAppLocales(composeResourcesDirectory) }
    }

    @Test
    fun givenALanguageFolderWithoutAppName_whenReadAppLocalesIsCalled_thenItFails() {
        val plDirectory = File(composeResourcesDirectory, "values-pl").apply { mkdirs() }
        File(plDirectory, "strings.xml").writeText(
            """
            <resources>
                <string name="other">Something</string>
            </resources>
            """.trimIndent(),
        )

        assertFailsWith<GradleException> { readAppLocales(composeResourcesDirectory) }
    }

    @Test
    fun givenLocalesInArbitraryOrder_whenReadAppLocalesIsCalled_thenDefaultIsFirstThenSortedByTag() {
        writeStrings("values-pl", "Repz")
        writeStrings("values-pt-rBR", "Repz")
        writeStrings("values", "Repz")

        val locales = readAppLocales(composeResourcesDirectory)

        assertEquals(listOf("en", "pl", "pt-BR"), locales.map { it.tag })
    }

    private fun writeStrings(directoryName: String, appName: String) {
        val directory = File(composeResourcesDirectory, directoryName).apply { mkdirs() }
        File(directory, "strings.xml").writeText(
            """
            <resources>
                <string name="app_name">$appName</string>
            </resources>
            """.trimIndent(),
        )
    }
}
