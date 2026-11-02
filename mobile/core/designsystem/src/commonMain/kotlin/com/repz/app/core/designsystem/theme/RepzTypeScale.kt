package com.repz.app.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Inter's bundled variable instance only supports opsz 14–32 (DESIGN_SYSTEM §4).
private const val INTER_OPTICAL_SIZE_MIN = 14f
private const val INTER_OPTICAL_SIZE_MAX = 32f

/**
 * One M3 or numeric style's raw spec (DESIGN_SYSTEM §4). Resolving a spec against a base
 * [TextStyle] (`TextStyle.copy(...)`) keeps the M3 default style's `lineHeightStyle` and
 * `platformStyle` trims, which are identical across every M3 token (they all copy the same
 * internal default), so any base style works; only font, size, line height, tracking and tabular
 * figures are overridden here.
 */
internal data class RepzTypeSpec(
    val font: RepzFontSpec,
    val sizeSp: Float,
    val lineHeightSp: Float,
    val trackingSp: Float,
    val tabularFigures: Boolean = false,
)

private fun archivoStyle(
    weight: Int,
    sizeSp: Float,
    lineHeightSp: Float,
    trackingSp: Float,
    width: Float = 100f,
    tabularFigures: Boolean = false,
) = RepzTypeSpec(
    font = RepzFontSpec(RepzFontFile.Archivo, weight = weight, width = width),
    sizeSp = sizeSp,
    lineHeightSp = lineHeightSp,
    trackingSp = trackingSp,
    tabularFigures = tabularFigures,
)

private fun interStyle(
    weight: Int,
    sizeSp: Float,
    lineHeightSp: Float,
    trackingSp: Float,
) = RepzTypeSpec(
    font =
        RepzFontSpec(
            RepzFontFile.Inter,
            weight = weight,
            opticalSize = sizeSp.coerceIn(INTER_OPTICAL_SIZE_MIN, INTER_OPTICAL_SIZE_MAX),
        ),
    sizeSp = sizeSp,
    lineHeightSp = lineHeightSp,
    trackingSp = trackingSp,
)

/**
 * The 34 styles (15 baseline + 15 emphasized + 4 numeric) from DESIGN_SYSTEM §4.
 *
 * Emphasized rule (DESIGN_SYSTEM §4, board 02): display = Archivo 850, wdth 112, tracking = base
 * − 0.5; headline = Archivo 850, wdth 108, tracking = base − 0.25; title/body/label = Inter 700,
 * same tracking as the baseline style. Numeric wdth defaults to 100 except numericHero (112);
 * numericMedium tracking is 0 (both from board 02, recorded in DESIGN_SYSTEM §4).
 */
internal object RepzTypeScale {
    val displayLarge = archivoStyle(weight = 700, sizeSp = 57f, lineHeightSp = 64f, trackingSp = -1f)
    val displayMedium = archivoStyle(weight = 700, sizeSp = 45f, lineHeightSp = 52f, trackingSp = -0.5f)
    val displaySmall = archivoStyle(weight = 700, sizeSp = 36f, lineHeightSp = 44f, trackingSp = -0.25f)
    val headlineLarge = archivoStyle(weight = 700, sizeSp = 32f, lineHeightSp = 40f, trackingSp = -0.25f)
    val headlineMedium = archivoStyle(weight = 700, sizeSp = 28f, lineHeightSp = 36f, trackingSp = 0f)
    val headlineSmall = archivoStyle(weight = 700, sizeSp = 24f, lineHeightSp = 32f, trackingSp = 0f)
    val titleLarge = interStyle(weight = 600, sizeSp = 22f, lineHeightSp = 28f, trackingSp = 0f)
    val titleMedium = interStyle(weight = 600, sizeSp = 16f, lineHeightSp = 24f, trackingSp = 0.15f)
    val titleSmall = interStyle(weight = 600, sizeSp = 14f, lineHeightSp = 20f, trackingSp = 0.1f)
    val bodyLarge = interStyle(weight = 400, sizeSp = 16f, lineHeightSp = 24f, trackingSp = 0.15f)
    val bodyMedium = interStyle(weight = 400, sizeSp = 14f, lineHeightSp = 20f, trackingSp = 0.2f)
    val bodySmall = interStyle(weight = 400, sizeSp = 12f, lineHeightSp = 16f, trackingSp = 0.3f)
    val labelLarge = interStyle(weight = 600, sizeSp = 14f, lineHeightSp = 20f, trackingSp = 0.1f)
    val labelMedium = interStyle(weight = 600, sizeSp = 12f, lineHeightSp = 16f, trackingSp = 0.4f)
    val labelSmall = interStyle(weight = 600, sizeSp = 11f, lineHeightSp = 16f, trackingSp = 0.5f)

    val displayLargeEmphasized =
        archivoStyle(weight = 850, sizeSp = 57f, lineHeightSp = 64f, trackingSp = -1.5f, width = 112f)
    val displayMediumEmphasized =
        archivoStyle(weight = 850, sizeSp = 45f, lineHeightSp = 52f, trackingSp = -1f, width = 112f)
    val displaySmallEmphasized =
        archivoStyle(weight = 850, sizeSp = 36f, lineHeightSp = 44f, trackingSp = -0.75f, width = 112f)
    val headlineLargeEmphasized =
        archivoStyle(weight = 850, sizeSp = 32f, lineHeightSp = 40f, trackingSp = -0.5f, width = 108f)
    val headlineMediumEmphasized =
        archivoStyle(weight = 850, sizeSp = 28f, lineHeightSp = 36f, trackingSp = -0.25f, width = 108f)
    val headlineSmallEmphasized =
        archivoStyle(weight = 850, sizeSp = 24f, lineHeightSp = 32f, trackingSp = -0.25f, width = 108f)
    val titleLargeEmphasized = interStyle(weight = 700, sizeSp = 22f, lineHeightSp = 28f, trackingSp = 0f)
    val titleMediumEmphasized = interStyle(weight = 700, sizeSp = 16f, lineHeightSp = 24f, trackingSp = 0.15f)
    val titleSmallEmphasized = interStyle(weight = 700, sizeSp = 14f, lineHeightSp = 20f, trackingSp = 0.1f)
    val bodyLargeEmphasized = interStyle(weight = 700, sizeSp = 16f, lineHeightSp = 24f, trackingSp = 0.15f)
    val bodyMediumEmphasized = interStyle(weight = 700, sizeSp = 14f, lineHeightSp = 20f, trackingSp = 0.2f)
    val bodySmallEmphasized = interStyle(weight = 700, sizeSp = 12f, lineHeightSp = 16f, trackingSp = 0.3f)
    val labelLargeEmphasized = interStyle(weight = 700, sizeSp = 14f, lineHeightSp = 20f, trackingSp = 0.1f)
    val labelMediumEmphasized = interStyle(weight = 700, sizeSp = 12f, lineHeightSp = 16f, trackingSp = 0.4f)
    val labelSmallEmphasized = interStyle(weight = 700, sizeSp = 11f, lineHeightSp = 16f, trackingSp = 0.5f)

    val numericHero =
        archivoStyle(weight = 800, sizeSp = 96f, lineHeightSp = 96f, trackingSp = -2f, width = 112f, tabularFigures = true)
    val numericLarge =
        archivoStyle(weight = 700, sizeSp = 32f, lineHeightSp = 36f, trackingSp = -0.5f, tabularFigures = true)
    val numericMedium =
        archivoStyle(weight = 650, sizeSp = 22f, lineHeightSp = 28f, trackingSp = 0f, tabularFigures = true)
    val numericSmall =
        archivoStyle(weight = 600, sizeSp = 14f, lineHeightSp = 20f, trackingSp = 0.1f, tabularFigures = true)

    val baselineAndEmphasized: List<RepzTypeSpec> =
        listOf(
            displayLarge,
            displayMedium,
            displaySmall,
            headlineLarge,
            headlineMedium,
            headlineSmall,
            titleLarge,
            titleMedium,
            titleSmall,
            bodyLarge,
            bodyMedium,
            bodySmall,
            labelLarge,
            labelMedium,
            labelSmall,
            displayLargeEmphasized,
            displayMediumEmphasized,
            displaySmallEmphasized,
            headlineLargeEmphasized,
            headlineMediumEmphasized,
            headlineSmallEmphasized,
            titleLargeEmphasized,
            titleMediumEmphasized,
            titleSmallEmphasized,
            bodyLargeEmphasized,
            bodyMediumEmphasized,
            bodySmallEmphasized,
            labelLargeEmphasized,
            labelMediumEmphasized,
            labelSmallEmphasized,
        )

    val numeric: List<RepzTypeSpec> = listOf(numericHero, numericLarge, numericMedium, numericSmall)

    val all: List<RepzTypeSpec> = baselineAndEmphasized + numeric

    val distinctFontSpecs: List<RepzFontSpec> = all.map { it.font }.distinct()
}

private fun RepzTypeSpec.resolve(
    base: TextStyle,
    families: RepzFontFamilies,
): TextStyle =
    base.copy(
        fontFamily = families.familyFor(font),
        fontWeight = FontWeight(font.weight),
        fontSize = sizeSp.sp,
        lineHeight = lineHeightSp.sp,
        letterSpacing = trackingSp.sp,
        fontFeatureSettings = if (tabularFigures) "tnum" else null,
    )

/** Builds the M3 [Typography] (15 baseline + 15 emphasized styles) from [families]. */
internal fun repzMaterialTypography(families: RepzFontFamilies): Typography {
    val base = Typography()
    return Typography(
        displayLarge = RepzTypeScale.displayLarge.resolve(base.displayLarge, families),
        displayMedium = RepzTypeScale.displayMedium.resolve(base.displayMedium, families),
        displaySmall = RepzTypeScale.displaySmall.resolve(base.displaySmall, families),
        headlineLarge = RepzTypeScale.headlineLarge.resolve(base.headlineLarge, families),
        headlineMedium = RepzTypeScale.headlineMedium.resolve(base.headlineMedium, families),
        headlineSmall = RepzTypeScale.headlineSmall.resolve(base.headlineSmall, families),
        titleLarge = RepzTypeScale.titleLarge.resolve(base.titleLarge, families),
        titleMedium = RepzTypeScale.titleMedium.resolve(base.titleMedium, families),
        titleSmall = RepzTypeScale.titleSmall.resolve(base.titleSmall, families),
        bodyLarge = RepzTypeScale.bodyLarge.resolve(base.bodyLarge, families),
        bodyMedium = RepzTypeScale.bodyMedium.resolve(base.bodyMedium, families),
        bodySmall = RepzTypeScale.bodySmall.resolve(base.bodySmall, families),
        labelLarge = RepzTypeScale.labelLarge.resolve(base.labelLarge, families),
        labelMedium = RepzTypeScale.labelMedium.resolve(base.labelMedium, families),
        labelSmall = RepzTypeScale.labelSmall.resolve(base.labelSmall, families),
        displayLargeEmphasized = RepzTypeScale.displayLargeEmphasized.resolve(base.displayLargeEmphasized, families),
        displayMediumEmphasized = RepzTypeScale.displayMediumEmphasized.resolve(base.displayMediumEmphasized, families),
        displaySmallEmphasized = RepzTypeScale.displaySmallEmphasized.resolve(base.displaySmallEmphasized, families),
        headlineLargeEmphasized = RepzTypeScale.headlineLargeEmphasized.resolve(base.headlineLargeEmphasized, families),
        headlineMediumEmphasized =
            RepzTypeScale.headlineMediumEmphasized.resolve(base.headlineMediumEmphasized, families),
        headlineSmallEmphasized = RepzTypeScale.headlineSmallEmphasized.resolve(base.headlineSmallEmphasized, families),
        titleLargeEmphasized = RepzTypeScale.titleLargeEmphasized.resolve(base.titleLargeEmphasized, families),
        titleMediumEmphasized = RepzTypeScale.titleMediumEmphasized.resolve(base.titleMediumEmphasized, families),
        titleSmallEmphasized = RepzTypeScale.titleSmallEmphasized.resolve(base.titleSmallEmphasized, families),
        bodyLargeEmphasized = RepzTypeScale.bodyLargeEmphasized.resolve(base.bodyLargeEmphasized, families),
        bodyMediumEmphasized = RepzTypeScale.bodyMediumEmphasized.resolve(base.bodyMediumEmphasized, families),
        bodySmallEmphasized = RepzTypeScale.bodySmallEmphasized.resolve(base.bodySmallEmphasized, families),
        labelLargeEmphasized = RepzTypeScale.labelLargeEmphasized.resolve(base.labelLargeEmphasized, families),
        labelMediumEmphasized = RepzTypeScale.labelMediumEmphasized.resolve(base.labelMediumEmphasized, families),
        labelSmallEmphasized = RepzTypeScale.labelSmallEmphasized.resolve(base.labelSmallEmphasized, families),
    )
}

/** Builds the 4 numeric styles (DESIGN_SYSTEM §4) from [families]. Numbers are a display-category
 * use of Archivo (DESIGN_SYSTEM §4 intro), so every numeric style resolves against the M3 default
 * `displayLarge` base; the default trims it carries are identical across every M3 style. */
internal fun repzNumericTypography(families: RepzFontFamilies): RepzTypography {
    val base = Typography().displayLarge
    return RepzTypography(
        numericHero = RepzTypeScale.numericHero.resolve(base, families),
        numericLarge = RepzTypeScale.numericLarge.resolve(base, families),
        numericMedium = RepzTypeScale.numericMedium.resolve(base, families),
        numericSmall = RepzTypeScale.numericSmall.resolve(base, families),
    )
}
