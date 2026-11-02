package com.repz.app.core.designsystem.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RepzCatalogPageTest {
    @Test
    fun given_everyPageKey_whenFromKey_thenRoundTripsToThatPage() {
        for (page in RepzCatalogPage.entries) {
            assertEquals(page, RepzCatalogPage.fromKey(page.key))
        }
    }

    @Test
    fun given_unknownKey_whenFromKey_thenReturnsNull() {
        assertNull(RepzCatalogPage.fromKey("nope"))
    }

    @Test
    fun given_nullKey_whenFromKey_thenReturnsNull() {
        assertNull(RepzCatalogPage.fromKey(null))
    }
}
