package com.repz.buildlogic.android

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComposeResourcesPackagingGuardTest {
    private lateinit var composeResourcesDirectory: File
    private lateinit var mergedAssetsRoot: File

    @BeforeTest
    fun setUp() {
        composeResourcesDirectory = createTempDirectory("compose-resources-source").toFile()
        mergedAssetsRoot = createTempDirectory("merged-android-assets").toFile()
    }

    @AfterTest
    fun tearDown() {
        composeResourcesDirectory.deleteRecursively()
        mergedAssetsRoot.deleteRecursively()
    }

    @Test
    fun givenEveryDeclaredFileIsMerged_whenFindMissingComposeResourcesIsCalled_thenNothingIsMissing() {
        writeSourceFile("font/archivo_variable.ttf")
        writeMergedFile("composeResources/com.repz.app.generated.resources/font/archivo_variable.ttf")

        val missing = findMissingComposeResources(composeResourcesDirectory, listOf(mergedAssetsRoot))

        assertTrue(missing.isEmpty())
    }

    @Test
    fun givenAFontMissingFromTheMergedAssets_whenFindMissingComposeResourcesIsCalled_thenItIsReported() {
        writeSourceFile("font/archivo_variable.ttf")
        writeSourceFile("font/inter_variable.ttf")
        writeMergedFile("composeResources/com.repz.app.generated.resources/font/archivo_variable.ttf")

        val missing = findMissingComposeResources(composeResourcesDirectory, listOf(mergedAssetsRoot))

        assertEquals(listOf("font/inter_variable.ttf"), missing)
    }

    @Test
    fun givenNoMergedAssetsAtAll_whenFindMissingComposeResourcesIsCalled_thenEveryDeclaredFileIsReported() {
        writeSourceFile("font/archivo_variable.ttf")
        writeSourceFile("files/licenses/archivo_ofl.txt")

        val missing = findMissingComposeResources(composeResourcesDirectory, emptyList())

        assertEquals(setOf("font/archivo_variable.ttf", "files/licenses/archivo_ofl.txt"), missing.toSet())
    }

    @Test
    fun givenNoDeclaredComposeResources_whenFindMissingComposeResourcesIsCalled_thenNothingIsMissing() {
        val missing = findMissingComposeResources(composeResourcesDirectory, listOf(mergedAssetsRoot))

        assertTrue(missing.isEmpty())
    }

    @Test
    fun givenAnUnrelatedFileSharingOnlyAFileName_whenFindMissingComposeResourcesIsCalled_thenItDoesNotCountAsAMatch() {
        writeSourceFile("font/archivo_variable.ttf")
        writeMergedFile("composeResources/other-module/some/other/path/archivo_variable.ttf")

        val missing = findMissingComposeResources(composeResourcesDirectory, listOf(mergedAssetsRoot))

        assertEquals(listOf("font/archivo_variable.ttf"), missing)
    }

    @Test
    fun givenAValuesStringsXmlCompiledToACvrFile_whenFindMissingComposeResourcesIsCalled_thenItCountsAsPackaged() {
        // Compose Multiplatform compiles `values/strings.xml` into a binary `strings.commonMain.cvr`
        // with no name relationship to the source file, so only the `values` directory is checked.
        writeSourceFile("values/strings.xml")
        writeMergedFile("composeResources/com.repz.app.generated.resources/values/strings.commonMain.cvr")

        val missing = findMissingComposeResources(composeResourcesDirectory, listOf(mergedAssetsRoot))

        assertTrue(missing.isEmpty())
    }

    @Test
    fun givenAValuesPlDirectoryMissingFromTheMergedAssets_whenFindMissingComposeResourcesIsCalled_thenItIsReported() {
        writeSourceFile("values/strings.xml")
        writeSourceFile("values-pl/strings.xml")
        writeMergedFile("composeResources/com.repz.app.generated.resources/values/strings.commonMain.cvr")

        val missing = findMissingComposeResources(composeResourcesDirectory, listOf(mergedAssetsRoot))

        assertEquals(listOf("values-pl/strings.xml"), missing)
    }

    private fun writeSourceFile(relativePath: String) {
        File(composeResourcesDirectory, relativePath).apply {
            parentFile.mkdirs()
            writeBytes(ByteArray(1))
        }
    }

    private fun writeMergedFile(relativePath: String) {
        File(mergedAssetsRoot, relativePath).apply {
            parentFile.mkdirs()
            writeBytes(ByteArray(1))
        }
    }
}
