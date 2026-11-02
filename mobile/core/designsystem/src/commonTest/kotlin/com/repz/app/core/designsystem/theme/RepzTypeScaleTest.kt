package com.repz.app.core.designsystem.theme

import androidx.compose.ui.text.font.FontFamily
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private data class ExpectedTypeSpec(
    val name: String,
    val actual: RepzTypeSpec,
    val file: RepzFontFile,
    val weight: Int,
    val width: Float,
    val opticalSize: Float?,
    val sizeSp: Float,
    val lineHeightSp: Float,
    val trackingSp: Float,
    val tabularFigures: Boolean = false,
)

class RepzTypeScaleTest {
    @Test
    fun given_baselineStyles_whenReadingSpecs_thenMatchDesignSystem() {
        assertAll(baselineSpecs)
    }

    @Test
    fun given_emphasizedStyles_whenReadingSpecs_thenMatchApprovedRule() {
        assertAll(emphasizedSpecs)
    }

    @Test
    fun given_numericStyles_whenReadingSpecs_thenMatchDesignSystemAndUseTabularFigures() {
        assertAll(numericSpecs)
    }

    @Test
    fun given_materialTypography_whenBuilt_thenEveryStyleUsesTheRequestedFamilyAndTabularFiguresOnlyOnNumeric() {
        val fakeFamilies = FakeRepzFontFamilies()

        val typography = repzMaterialTypography(fakeFamilies)
        val numericTypography = repzNumericTypography(fakeFamilies)

        // Every distinct spec in the type scale was requested exactly once per style, so the
        // requested set equals the full distinct-spec set.
        assertEquals(RepzTypeScale.distinctFontSpecs.toSet(), fakeFamilies.requestedSpecs.toSet())

        assertEquals(RepzTypeScale.displayLarge.sizeSp, typography.displayLarge.fontSize.value)
        assertEquals(RepzTypeScale.displayLarge.lineHeightSp, typography.displayLarge.lineHeight.value)
        assertEquals(RepzTypeScale.displayLarge.trackingSp, typography.displayLarge.letterSpacing.value)
        assertNull(typography.displayLarge.fontFeatureSettings)

        assertEquals(RepzTypeScale.labelSmallEmphasized.sizeSp, typography.labelSmallEmphasized.fontSize.value)
        assertNull(typography.labelSmallEmphasized.fontFeatureSettings)

        assertEquals("tnum", numericTypography.numericHero.fontFeatureSettings)
        assertEquals("tnum", numericTypography.numericLarge.fontFeatureSettings)
        assertEquals("tnum", numericTypography.numericMedium.fontFeatureSettings)
        assertEquals("tnum", numericTypography.numericSmall.fontFeatureSettings)
        assertEquals(RepzTypeScale.numericHero.trackingSp, numericTypography.numericHero.letterSpacing.value)
    }

    private fun assertAll(specs: List<ExpectedTypeSpec>) {
        for (expected in specs) {
            assertSpec(expected)
        }
    }

    private fun assertSpec(expected: ExpectedTypeSpec) {
        val actual = expected.actual
        val name = expected.name
        assertEquals(expected.file, actual.font.file, "$name.font.file")
        assertEquals(expected.weight, actual.font.weight, "$name.font.weight")
        assertEquals(expected.width, actual.font.width, "$name.font.width")
        assertEquals(expected.opticalSize, actual.font.opticalSize, "$name.font.opticalSize")
        assertEquals(expected.sizeSp, actual.sizeSp, "$name.sizeSp")
        assertEquals(expected.lineHeightSp, actual.lineHeightSp, "$name.lineHeightSp")
        assertEquals(expected.trackingSp, actual.trackingSp, "$name.trackingSp")
        assertEquals(expected.tabularFigures, actual.tabularFigures, "$name.tabularFigures")
        expected.opticalSize?.let {
            assertTrue(it in 14f..32f, "$name.font.opticalSize must be clamped to 14..32")
        }
    }
}

private val baselineSpecs =
    listOf(
        ExpectedTypeSpec("displayLarge", RepzTypeScale.displayLarge, RepzFontFile.Archivo, 700, 100f, null, 57f, 64f, -1f),
        ExpectedTypeSpec("displayMedium", RepzTypeScale.displayMedium, RepzFontFile.Archivo, 700, 100f, null, 45f, 52f, -0.5f),
        ExpectedTypeSpec("displaySmall", RepzTypeScale.displaySmall, RepzFontFile.Archivo, 700, 100f, null, 36f, 44f, -0.25f),
        ExpectedTypeSpec("headlineLarge", RepzTypeScale.headlineLarge, RepzFontFile.Archivo, 700, 100f, null, 32f, 40f, -0.25f),
        ExpectedTypeSpec("headlineMedium", RepzTypeScale.headlineMedium, RepzFontFile.Archivo, 700, 100f, null, 28f, 36f, 0f),
        ExpectedTypeSpec("headlineSmall", RepzTypeScale.headlineSmall, RepzFontFile.Archivo, 700, 100f, null, 24f, 32f, 0f),
        ExpectedTypeSpec("titleLarge", RepzTypeScale.titleLarge, RepzFontFile.Inter, 600, 100f, 22f, 22f, 28f, 0f),
        ExpectedTypeSpec("titleMedium", RepzTypeScale.titleMedium, RepzFontFile.Inter, 600, 100f, 16f, 16f, 24f, 0.15f),
        ExpectedTypeSpec("titleSmall", RepzTypeScale.titleSmall, RepzFontFile.Inter, 600, 100f, 14f, 14f, 20f, 0.1f),
        ExpectedTypeSpec("bodyLarge", RepzTypeScale.bodyLarge, RepzFontFile.Inter, 400, 100f, 16f, 16f, 24f, 0.15f),
        ExpectedTypeSpec("bodyMedium", RepzTypeScale.bodyMedium, RepzFontFile.Inter, 400, 100f, 14f, 14f, 20f, 0.2f),
        ExpectedTypeSpec("bodySmall", RepzTypeScale.bodySmall, RepzFontFile.Inter, 400, 100f, 14f, 12f, 16f, 0.3f),
        ExpectedTypeSpec("labelLarge", RepzTypeScale.labelLarge, RepzFontFile.Inter, 600, 100f, 14f, 14f, 20f, 0.1f),
        ExpectedTypeSpec("labelMedium", RepzTypeScale.labelMedium, RepzFontFile.Inter, 600, 100f, 14f, 12f, 16f, 0.4f),
        ExpectedTypeSpec("labelSmall", RepzTypeScale.labelSmall, RepzFontFile.Inter, 600, 100f, 14f, 11f, 16f, 0.5f),
    )

private val emphasizedSpecs =
    listOf(
        ExpectedTypeSpec(
            "displayLargeEmphasized",
            RepzTypeScale.displayLargeEmphasized,
            RepzFontFile.Archivo,
            850,
            112f,
            null,
            57f,
            64f,
            -1.5f,
        ),
        ExpectedTypeSpec(
            "displayMediumEmphasized",
            RepzTypeScale.displayMediumEmphasized,
            RepzFontFile.Archivo,
            850,
            112f,
            null,
            45f,
            52f,
            -1f,
        ),
        ExpectedTypeSpec(
            "displaySmallEmphasized",
            RepzTypeScale.displaySmallEmphasized,
            RepzFontFile.Archivo,
            850,
            112f,
            null,
            36f,
            44f,
            -0.75f,
        ),
        ExpectedTypeSpec(
            "headlineLargeEmphasized",
            RepzTypeScale.headlineLargeEmphasized,
            RepzFontFile.Archivo,
            850,
            108f,
            null,
            32f,
            40f,
            -0.5f,
        ),
        ExpectedTypeSpec(
            "headlineMediumEmphasized",
            RepzTypeScale.headlineMediumEmphasized,
            RepzFontFile.Archivo,
            850,
            108f,
            null,
            28f,
            36f,
            -0.25f,
        ),
        ExpectedTypeSpec(
            "headlineSmallEmphasized",
            RepzTypeScale.headlineSmallEmphasized,
            RepzFontFile.Archivo,
            850,
            108f,
            null,
            24f,
            32f,
            -0.25f,
        ),
        ExpectedTypeSpec(
            "titleLargeEmphasized",
            RepzTypeScale.titleLargeEmphasized,
            RepzFontFile.Inter,
            700,
            100f,
            22f,
            22f,
            28f,
            0f,
        ),
        ExpectedTypeSpec(
            "titleMediumEmphasized",
            RepzTypeScale.titleMediumEmphasized,
            RepzFontFile.Inter,
            700,
            100f,
            16f,
            16f,
            24f,
            0.15f,
        ),
        ExpectedTypeSpec(
            "titleSmallEmphasized",
            RepzTypeScale.titleSmallEmphasized,
            RepzFontFile.Inter,
            700,
            100f,
            14f,
            14f,
            20f,
            0.1f,
        ),
        ExpectedTypeSpec(
            "bodyLargeEmphasized",
            RepzTypeScale.bodyLargeEmphasized,
            RepzFontFile.Inter,
            700,
            100f,
            16f,
            16f,
            24f,
            0.15f,
        ),
        ExpectedTypeSpec(
            "bodyMediumEmphasized",
            RepzTypeScale.bodyMediumEmphasized,
            RepzFontFile.Inter,
            700,
            100f,
            14f,
            14f,
            20f,
            0.2f,
        ),
        ExpectedTypeSpec(
            "bodySmallEmphasized",
            RepzTypeScale.bodySmallEmphasized,
            RepzFontFile.Inter,
            700,
            100f,
            14f,
            12f,
            16f,
            0.3f,
        ),
        ExpectedTypeSpec(
            "labelLargeEmphasized",
            RepzTypeScale.labelLargeEmphasized,
            RepzFontFile.Inter,
            700,
            100f,
            14f,
            14f,
            20f,
            0.1f,
        ),
        ExpectedTypeSpec(
            "labelMediumEmphasized",
            RepzTypeScale.labelMediumEmphasized,
            RepzFontFile.Inter,
            700,
            100f,
            14f,
            12f,
            16f,
            0.4f,
        ),
        ExpectedTypeSpec(
            "labelSmallEmphasized",
            RepzTypeScale.labelSmallEmphasized,
            RepzFontFile.Inter,
            700,
            100f,
            14f,
            11f,
            16f,
            0.5f,
        ),
    )

private val numericSpecs =
    listOf(
        ExpectedTypeSpec(
            "numericHero",
            RepzTypeScale.numericHero,
            RepzFontFile.Archivo,
            800,
            112f,
            null,
            96f,
            96f,
            -2f,
            tabularFigures = true,
        ),
        ExpectedTypeSpec(
            "numericLarge",
            RepzTypeScale.numericLarge,
            RepzFontFile.Archivo,
            700,
            100f,
            null,
            32f,
            36f,
            -0.5f,
            tabularFigures = true,
        ),
        ExpectedTypeSpec(
            "numericMedium",
            RepzTypeScale.numericMedium,
            RepzFontFile.Archivo,
            650,
            100f,
            null,
            22f,
            28f,
            0f,
            tabularFigures = true,
        ),
        ExpectedTypeSpec(
            "numericSmall",
            RepzTypeScale.numericSmall,
            RepzFontFile.Archivo,
            600,
            100f,
            null,
            14f,
            20f,
            0.1f,
            tabularFigures = true,
        ),
    )

/** Records every [RepzFontSpec] it is asked to resolve; returns a stable [FontFamily] per spec. */
private class FakeRepzFontFamilies : RepzFontFamilies {
    val requestedSpecs = mutableListOf<RepzFontSpec>()
    private val families = mutableMapOf<RepzFontSpec, FontFamily>()

    override fun familyFor(spec: RepzFontSpec): FontFamily {
        requestedSpecs += spec
        return families.getOrPut(spec) { FontFamily.Default }
    }
}
