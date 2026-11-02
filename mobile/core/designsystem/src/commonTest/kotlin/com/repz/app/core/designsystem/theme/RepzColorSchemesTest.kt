package com.repz.app.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.repz.app.core.designsystem.RepzBrandColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class RepzColorSchemesTest {
    @Test
    fun given_lightScheme_whenReadingEveryRole_thenMatchesDesignSystem() {
        assertSchemeMatches(
            scheme = RepzLightColorScheme,
            primary = 0xFF006879,
            onPrimary = 0xFFFFFFFF,
            primaryContainer = 0xFF00C2E0,
            onPrimaryContainer = 0xFF0E1416,
            inversePrimary = 0xFF3CD8F6,
            secondary = 0xFF356570,
            onSecondary = 0xFFFFFFFF,
            secondaryContainer = 0xFFB7E8F5,
            onSecondaryContainer = 0xFF061F24,
            tertiary = 0xFF8D4F00,
            onTertiary = 0xFFFFFFFF,
            tertiaryContainer = 0xFFF79C3C,
            onTertiaryContainer = 0xFF2E1500,
            error = 0xFFBA1A1A,
            onError = 0xFFFFFFFF,
            errorContainer = 0xFFFFDAD6,
            onErrorContainer = 0xFF93000A,
            background = 0xFFF4FAFD,
            onBackground = 0xFF161D1E,
            surface = 0xFFF4FAFD,
            onSurface = 0xFF161D1E,
            surfaceVariant = 0xFFD7E5E9,
            onSurfaceVariant = 0xFF3C494C,
            surfaceTint = 0xFF006879,
            inverseSurface = 0xFF2B3133,
            inverseOnSurface = 0xFFECF2F4,
            outline = 0xFF6C797D,
            outlineVariant = 0xFFBBC9CD,
            scrim = 0xFF000000,
            surfaceBright = 0xFFF4FAFD,
            surfaceDim = 0xFFD5DBDD,
            surfaceContainerLowest = 0xFFFFFFFF,
            surfaceContainerLow = 0xFFEEF5F7,
            surfaceContainer = 0xFFE9EFF1,
            surfaceContainerHigh = 0xFFE3E9EB,
            surfaceContainerHighest = 0xFFDDE3E6,
        )
    }

    @Test
    fun given_darkScheme_whenReadingEveryRole_thenMatchesDesignSystem() {
        assertSchemeMatches(
            scheme = RepzDarkColorScheme,
            primary = 0xFF46DEFD,
            onPrimary = 0xFF003640,
            primaryContainer = 0xFF00C2E0,
            onPrimaryContainer = 0xFF0E1416,
            inversePrimary = 0xFF006879,
            secondary = 0xFF9ECEDB,
            onSecondary = 0xFF003640,
            secondaryContainer = 0xFF1A4D58,
            onSecondaryContainer = 0xFFCEE7EE,
            tertiary = 0xFFFFC086,
            onTertiary = 0xFF4B2800,
            tertiaryContainer = 0xFFF79C3C,
            onTertiaryContainer = 0xFF2E1500,
            error = 0xFFFFB4AB,
            onError = 0xFF690005,
            errorContainer = 0xFF93000A,
            onErrorContainer = 0xFFFFDAD6,
            background = 0xFF0E1416,
            onBackground = 0xFFDDE3E6,
            surface = 0xFF0E1416,
            onSurface = 0xFFDDE3E6,
            surfaceVariant = 0xFF3C494C,
            onSurfaceVariant = 0xFFBBC9CD,
            surfaceTint = 0xFF3CD8F6,
            inverseSurface = 0xFFDDE3E6,
            inverseOnSurface = 0xFF2B3133,
            outline = 0xFF869397,
            outlineVariant = 0xFF3C494C,
            scrim = 0xFF000000,
            surfaceBright = 0xFF343A3C,
            surfaceDim = 0xFF0E1416,
            surfaceContainerLowest = 0xFF090F11,
            surfaceContainerLow = 0xFF161D1E,
            surfaceContainer = 0xFF1A2122,
            surfaceContainerHigh = 0xFF252B2D,
            surfaceContainerHighest = 0xFF2F3638,
        )
    }

    @Test
    fun given_brandScheme_whenReadingOnPrimaryContainer_thenIsBrandInk() {
        assertEquals(RepzBrandColors.Ink, RepzLightColorScheme.onPrimaryContainer)
        assertEquals(RepzBrandColors.Ink, RepzDarkColorScheme.onPrimaryContainer)
        assertEquals(RepzBrandColors.Cyan, RepzLightColorScheme.primaryContainer)
        assertEquals(RepzBrandColors.Cyan, RepzDarkColorScheme.primaryContainer)
    }

    @Test
    fun given_dynamicSchemePresent_whenSelecting_thenDynamicReturned() {
        val dynamicScheme = RepzDarkColorScheme

        val selectedLight = selectColorScheme(darkTheme = false, dynamicScheme = dynamicScheme)
        val selectedDark = selectColorScheme(darkTheme = true, dynamicScheme = dynamicScheme)

        assertSame(dynamicScheme, selectedLight)
        assertSame(dynamicScheme, selectedDark)
    }

    @Test
    fun given_noDynamicScheme_whenSelecting_thenBrandSchemeForThemeReturned() {
        assertSame(RepzLightColorScheme, selectColorScheme(darkTheme = false, dynamicScheme = null))
        assertSame(RepzDarkColorScheme, selectColorScheme(darkTheme = true, dynamicScheme = null))
    }

    private fun assertSchemeMatches(
        scheme: ColorScheme,
        primary: Long,
        onPrimary: Long,
        primaryContainer: Long,
        onPrimaryContainer: Long,
        inversePrimary: Long,
        secondary: Long,
        onSecondary: Long,
        secondaryContainer: Long,
        onSecondaryContainer: Long,
        tertiary: Long,
        onTertiary: Long,
        tertiaryContainer: Long,
        onTertiaryContainer: Long,
        error: Long,
        onError: Long,
        errorContainer: Long,
        onErrorContainer: Long,
        background: Long,
        onBackground: Long,
        surface: Long,
        onSurface: Long,
        surfaceVariant: Long,
        onSurfaceVariant: Long,
        surfaceTint: Long,
        inverseSurface: Long,
        inverseOnSurface: Long,
        outline: Long,
        outlineVariant: Long,
        scrim: Long,
        surfaceBright: Long,
        surfaceDim: Long,
        surfaceContainerLowest: Long,
        surfaceContainerLow: Long,
        surfaceContainer: Long,
        surfaceContainerHigh: Long,
        surfaceContainerHighest: Long,
    ) {
        assertNotNull(scheme)
        assertEquals(Color(primary), scheme.primary, "primary")
        assertEquals(Color(onPrimary), scheme.onPrimary, "onPrimary")
        assertEquals(Color(primaryContainer), scheme.primaryContainer, "primaryContainer")
        assertEquals(Color(onPrimaryContainer), scheme.onPrimaryContainer, "onPrimaryContainer")
        assertEquals(Color(inversePrimary), scheme.inversePrimary, "inversePrimary")
        assertEquals(Color(secondary), scheme.secondary, "secondary")
        assertEquals(Color(onSecondary), scheme.onSecondary, "onSecondary")
        assertEquals(Color(secondaryContainer), scheme.secondaryContainer, "secondaryContainer")
        assertEquals(Color(onSecondaryContainer), scheme.onSecondaryContainer, "onSecondaryContainer")
        assertEquals(Color(tertiary), scheme.tertiary, "tertiary")
        assertEquals(Color(onTertiary), scheme.onTertiary, "onTertiary")
        assertEquals(Color(tertiaryContainer), scheme.tertiaryContainer, "tertiaryContainer")
        assertEquals(Color(onTertiaryContainer), scheme.onTertiaryContainer, "onTertiaryContainer")
        assertEquals(Color(error), scheme.error, "error")
        assertEquals(Color(onError), scheme.onError, "onError")
        assertEquals(Color(errorContainer), scheme.errorContainer, "errorContainer")
        assertEquals(Color(onErrorContainer), scheme.onErrorContainer, "onErrorContainer")
        assertEquals(Color(background), scheme.background, "background")
        assertEquals(Color(onBackground), scheme.onBackground, "onBackground")
        assertEquals(Color(surface), scheme.surface, "surface")
        assertEquals(Color(onSurface), scheme.onSurface, "onSurface")
        assertEquals(Color(surfaceVariant), scheme.surfaceVariant, "surfaceVariant")
        assertEquals(Color(onSurfaceVariant), scheme.onSurfaceVariant, "onSurfaceVariant")
        assertEquals(Color(surfaceTint), scheme.surfaceTint, "surfaceTint")
        assertEquals(Color(inverseSurface), scheme.inverseSurface, "inverseSurface")
        assertEquals(Color(inverseOnSurface), scheme.inverseOnSurface, "inverseOnSurface")
        assertEquals(Color(outline), scheme.outline, "outline")
        assertEquals(Color(outlineVariant), scheme.outlineVariant, "outlineVariant")
        assertEquals(Color(scrim), scheme.scrim, "scrim")
        assertEquals(Color(surfaceBright), scheme.surfaceBright, "surfaceBright")
        assertEquals(Color(surfaceDim), scheme.surfaceDim, "surfaceDim")
        assertEquals(Color(surfaceContainerLowest), scheme.surfaceContainerLowest, "surfaceContainerLowest")
        assertEquals(Color(surfaceContainerLow), scheme.surfaceContainerLow, "surfaceContainerLow")
        assertEquals(Color(surfaceContainer), scheme.surfaceContainer, "surfaceContainer")
        assertEquals(Color(surfaceContainerHigh), scheme.surfaceContainerHigh, "surfaceContainerHigh")
        assertEquals(Color(surfaceContainerHighest), scheme.surfaceContainerHighest, "surfaceContainerHighest")
    }
}
