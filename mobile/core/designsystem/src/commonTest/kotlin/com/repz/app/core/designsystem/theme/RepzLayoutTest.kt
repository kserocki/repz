package com.repz.app.core.designsystem.theme

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class RepzLayoutTest {
    @Test
    fun given_repzLayout_whenReadingEveryToken_thenMatchesDesignSystemLayoutTable() {
        assertEquals(16.dp, RepzLayout.screenMarginCompact)
        assertEquals(24.dp, RepzLayout.screenMarginMedium)
        assertEquals(16.dp, RepzLayout.cardPadding)
        assertEquals(12.dp, RepzLayout.listGap)
        assertEquals(56.dp, RepzLayout.setRowHeight)
        assertEquals(64.dp, RepzLayout.setRowHeightLargeFont)
        assertEquals(56.dp, RepzLayout.listItemOneLineHeight)
        assertEquals(72.dp, RepzLayout.listItemTwoLineHeight)
        assertEquals(64.dp, RepzLayout.topAppBarSmallHeight)
        assertEquals(112.dp, RepzLayout.topAppBarMediumHeight)
        assertEquals(152.dp, RepzLayout.topAppBarLargeHeight)
        assertEquals(92.dp, RepzLayout.liveWorkoutHeaderHeight)
        assertEquals(64.dp, RepzLayout.miniBarHeight)
        assertEquals(80.dp, RepzLayout.navigationBarHeight)
        assertEquals(48.dp, RepzLayout.minTouchTarget)
        assertEquals(56.dp, RepzLayout.loggingTouchTarget)
        assertEquals(360.dp, RepzLayout.listPaneWidth)
        assertEquals(600.dp, RepzLayout.liveWorkoutMaxWidth)
    }
}
