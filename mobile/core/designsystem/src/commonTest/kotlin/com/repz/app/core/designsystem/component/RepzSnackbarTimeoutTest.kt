package com.repz.app.core.designsystem.component

import kotlin.test.Test
import kotlin.test.assertEquals

private const val UNDO_BASE_TIMEOUT_MILLIS = 6_000L

class RepzSnackbarTimeoutTest {
    @Test
    fun given_aRecommenderThatKeepsTheBase_whenComputingUndoTimeout_thenReturnsTheBase() {
        assertEquals(UNDO_BASE_TIMEOUT_MILLIS, undoSnackbarTimeoutMillis { base -> base })
    }

    @Test
    fun given_anAccessibilityRecommenderThatExtendsIt_whenComputingUndoTimeout_thenReturnsTheExtendedValue() {
        val extendedTimeoutMillis = 12_000L
        assertEquals(extendedTimeoutMillis, undoSnackbarTimeoutMillis { base -> base * 2 })
    }

    @Test
    fun given_anyRecommender_whenComputingUndoTimeout_thenPasses6000MillisAsTheBase() {
        var receivedBaseMillis: Long? = null
        undoSnackbarTimeoutMillis { base ->
            receivedBaseMillis = base
            base
        }

        assertEquals(UNDO_BASE_TIMEOUT_MILLIS, receivedBaseMillis)
    }
}
