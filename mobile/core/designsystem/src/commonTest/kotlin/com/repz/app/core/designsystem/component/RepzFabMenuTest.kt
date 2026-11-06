package com.repz.app.core.designsystem.component

import com.repz.app.core.designsystem.motion.RepzStagger
import kotlin.test.Test
import kotlin.test.assertEquals

class RepzFabMenuTest {
    @Test
    fun given_opening_whenReadingEnterDelayOfEachItem_thenBottomItemIsFirst() {
        // 3 items: index 2 (closest to the toggle, bottom of the stack) enters first, index 0
        // (farthest from the toggle, top of the stack) enters last (MOT-30c "bottom-up").
        assertEquals(RepzStagger.listPerItem * 2, fabMenuItemEnterDelay(index = 0, itemCount = 3, opening = true))
        assertEquals(RepzStagger.listPerItem * 1, fabMenuItemEnterDelay(index = 1, itemCount = 3, opening = true))
        assertEquals(RepzStagger.listPerItem * 0, fabMenuItemEnterDelay(index = 2, itemCount = 3, opening = true))
    }

    @Test
    fun given_closing_whenReadingEnterDelayOfEachItem_thenTopItemIsFirst() {
        // Reverse order on close (MOT-30c "top-down"): index 0 leaves first, index 2 leaves last.
        assertEquals(RepzStagger.listPerItem * 0, fabMenuItemEnterDelay(index = 0, itemCount = 3, opening = false))
        assertEquals(RepzStagger.listPerItem * 1, fabMenuItemEnterDelay(index = 1, itemCount = 3, opening = false))
        assertEquals(RepzStagger.listPerItem * 2, fabMenuItemEnterDelay(index = 2, itemCount = 3, opening = false))
    }

    @Test
    fun given_twoItemMenu_whenOpeningOrClosing_thenStepsAre30MsApart() {
        assertEquals(RepzStagger.listPerItem, fabMenuItemEnterDelay(index = 0, itemCount = 2, opening = true))
        assertEquals(RepzStagger.listPerItem * 0, fabMenuItemEnterDelay(index = 1, itemCount = 2, opening = true))
        assertEquals(RepzStagger.listPerItem * 0, fabMenuItemEnterDelay(index = 0, itemCount = 2, opening = false))
        assertEquals(RepzStagger.listPerItem, fabMenuItemEnterDelay(index = 1, itemCount = 2, opening = false))
    }

    @Test
    fun given_singleItemMenu_whenOpeningOrClosing_thenNoDelay() {
        assertEquals(RepzStagger.listPerItem * 0, fabMenuItemEnterDelay(index = 0, itemCount = 1, opening = true))
        assertEquals(RepzStagger.listPerItem * 0, fabMenuItemEnterDelay(index = 0, itemCount = 1, opening = false))
    }
}
