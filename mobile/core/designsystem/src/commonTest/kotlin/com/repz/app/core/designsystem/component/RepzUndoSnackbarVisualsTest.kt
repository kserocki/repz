package com.repz.app.core.designsystem.component

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RepzUndoSnackbarVisualsTest {
    @Test
    fun given_anUndoSnackbar_whenReadingDuration_thenIndefinite() {
        val visuals = RepzUndoSnackbarVisuals(message = "Set removed", actionLabel = "Undo")

        assertEquals(SnackbarDuration.Indefinite, visuals.duration)
        assertEquals("Set removed", visuals.message)
        assertEquals("Undo", visuals.actionLabel)
        assertFalse(visuals.withDismissAction)
    }

    @Test
    fun given_anUndoSnackbar_whenCheckingAutoDismiss_thenTrue() {
        val visuals = RepzUndoSnackbarVisuals(message = "Set removed", actionLabel = "Undo")

        assertTrue(visuals.autoDismissesAfterUndoTimeout())
    }

    @Test
    fun given_aShowWithActionStyleIndefiniteSnackbar_whenCheckingAutoDismiss_thenFalse() {
        // M3's own showSnackbar(message, actionLabel) defaults to Indefinite whenever an action
        // label is set, so duration alone can't tell a showWithAction snackbar apart from an undo
        // one. Declared as the common SnackbarVisuals supertype so the check below is a real
        // runtime check, not a statically-impossible one between two unrelated final classes.
        val retrySnackbar: SnackbarVisuals = FakeIndefiniteActionVisuals(message = "Couldn't save", actionLabel = "Retry")

        assertEquals(SnackbarDuration.Indefinite, retrySnackbar.duration)
        assertFalse(retrySnackbar.autoDismissesAfterUndoTimeout())
    }
}

private class FakeIndefiniteActionVisuals(
    override val message: String,
    override val actionLabel: String,
) : SnackbarVisuals {
    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration = SnackbarDuration.Indefinite
}
