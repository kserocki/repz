package com.repz.app.core.designsystem.theme

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class RepzIconSizeTest {
    @Test
    fun given_repzIconSize_whenReadingEveryToken_thenMatchesDesignSystemIconography() {
        assertEquals(20.dp, RepzIconSize.dense)
        assertEquals(24.dp, RepzIconSize.default)
        assertEquals(28.dp, RepzIconSize.navBar)
        assertEquals(16.dp, RepzIconSize.inline)
    }
}
