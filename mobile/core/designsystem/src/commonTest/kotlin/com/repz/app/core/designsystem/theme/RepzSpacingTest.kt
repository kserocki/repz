package com.repz.app.core.designsystem.theme

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class RepzSpacingTest {
    @Test
    fun given_repzSpacing_whenReadingEveryToken_thenMatchesDesignSystemGrid() {
        assertEquals(2.dp, RepzSpacing.xxs)
        assertEquals(4.dp, RepzSpacing.xs)
        assertEquals(8.dp, RepzSpacing.sm)
        assertEquals(12.dp, RepzSpacing.md)
        assertEquals(16.dp, RepzSpacing.lg)
        assertEquals(24.dp, RepzSpacing.xl)
        assertEquals(32.dp, RepzSpacing.xxl)
        assertEquals(48.dp, RepzSpacing.xxxl)
        assertEquals(64.dp, RepzSpacing.huge)
    }
}
