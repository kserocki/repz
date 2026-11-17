package com.repz.app.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The layout dimensions of DESIGN_SYSTEM §6: screen margins, list/row heights, bar heights and
 * the wide-window (≥ 840 dp) pane widths. Features never hard-code these dp values. */
object RepzLayout {
    val screenMarginCompact: Dp = 16.dp
    val screenMarginMedium: Dp = 24.dp
    val cardPadding: Dp = 16.dp
    val listGap: Dp = 12.dp
    val setRowHeight: Dp = 56.dp
    val setRowHeightLargeFont: Dp = 64.dp
    val listItemOneLineHeight: Dp = 56.dp
    val listItemTwoLineHeight: Dp = 72.dp
    val topAppBarSmallHeight: Dp = 64.dp
    val topAppBarMediumHeight: Dp = 112.dp
    val topAppBarLargeHeight: Dp = 152.dp
    val liveWorkoutHeaderHeight: Dp = 92.dp
    val miniBarHeight: Dp = 64.dp
    val navigationBarHeight: Dp = 80.dp
    val minTouchTarget: Dp = 48.dp
    val loggingTouchTarget: Dp = 56.dp
    val listPaneWidth: Dp = 360.dp
    val liveWorkoutMaxWidth: Dp = 600.dp
}
