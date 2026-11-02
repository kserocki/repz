package com.repz.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RepzElevationTest {
    @Test
    fun given_lightTheme_whenBuildingElevationScheme_thenLevelsMapToContainerRoles() {
        val scheme = repzElevationScheme(RepzLightColorScheme, darkTheme = false)

        assertEquals(RepzLightColorScheme.surface, scheme.level0.containerColor)
        assertEquals(RepzLightColorScheme.surfaceContainerLow, scheme.level1.containerColor)
        assertEquals(RepzLightColorScheme.surfaceContainer, scheme.level2.containerColor)
        assertEquals(RepzLightColorScheme.surfaceContainerHigh, scheme.level3.containerColor)
        assertEquals(RepzLightColorScheme.surfaceContainerHighest, scheme.level4.containerColor)
        assertEquals(RepzLightColorScheme.primaryContainer, scheme.fab.containerColor)
    }

    @Test
    fun given_darkTheme_whenBuildingElevationScheme_thenLevelsMapToContainerRoles() {
        val scheme = repzElevationScheme(RepzDarkColorScheme, darkTheme = true)

        assertEquals(RepzDarkColorScheme.surface, scheme.level0.containerColor)
        assertEquals(RepzDarkColorScheme.surfaceContainerLow, scheme.level1.containerColor)
        assertEquals(RepzDarkColorScheme.surfaceContainer, scheme.level2.containerColor)
        assertEquals(RepzDarkColorScheme.surfaceContainerHigh, scheme.level3.containerColor)
        assertEquals(RepzDarkColorScheme.surfaceContainerHighest, scheme.level4.containerColor)
        assertEquals(RepzDarkColorScheme.primaryContainer, scheme.fab.containerColor)
    }

    @Test
    fun given_anyTheme_whenBuildingElevationScheme_thenAmongLevelsOnlyLevel4HasAShadow() {
        val light = repzElevationScheme(RepzLightColorScheme, darkTheme = false)
        val dark = repzElevationScheme(RepzDarkColorScheme, darkTheme = true)

        for (scheme in listOf(light, dark)) {
            assertNull(scheme.level0.shadow)
            assertNull(scheme.level1.shadow)
            assertNull(scheme.level2.shadow)
            assertNull(scheme.level3.shadow)
            assertNotNull(scheme.level4.shadow)
        }

        val level4Shadow = light.level4.shadow
        assertNotNull(level4Shadow)
        assertEquals(20.dp, level4Shadow.radius)
        assertEquals(Color(0x66000000), level4Shadow.color)
        assertEquals(DpOffset(0.dp, 8.dp), level4Shadow.offset)
    }

    @Test
    fun given_lightTheme_whenBuildingElevationScheme_thenFabHasAShadow() {
        val scheme = repzElevationScheme(RepzLightColorScheme, darkTheme = false)

        val fabShadow = scheme.fab.shadow
        assertNotNull(fabShadow)
        assertEquals(12.dp, fabShadow.radius)
        assertEquals(Color(0x380E1416), fabShadow.color)
        assertEquals(DpOffset(0.dp, 4.dp), fabShadow.offset)
    }

    @Test
    fun given_darkTheme_whenBuildingElevationScheme_thenFabHasNoShadow() {
        val scheme = repzElevationScheme(RepzDarkColorScheme, darkTheme = true)

        assertNull(scheme.fab.shadow)
    }
}
