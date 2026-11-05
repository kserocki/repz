package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

// DESIGN_SYSTEM §7: sheets and dialogs use a scrim of `#000` at 32 %, over the M3 `scrim` role
// (`#000000` in both themes) instead of a hard-coded black.
private const val DIALOG_SCRIM_OPACITY = 0.32f

/**
 * A confirmation dialog (DESIGN_SYSTEM §9, board 25, Q4): M3 `AlertDialog` (ADR-0006) in the
 * `surfaceContainerHigh` container, `shapes.extraLarge` (28 dp), no icon, centred `headlineSmall`
 * [title] and centred `bodyMedium`/`onSurfaceVariant` [text]. The two text buttons are M3's own
 * end-aligned dialog row: [dismissText] (e.g. "Cancel", `primary`, M3's default text-button colour)
 * renders to the left of [confirmText] (`error` when [isConfirmDestructive], matching the F25
 * Log out row). [onDismissRequest] also fires on Back and a scrim tap, in addition to
 * [dismissText]'s own click — callers wire both to the same "cancel" handling. [confirmButtonModifier]
 * and [dismissButtonModifier] tag the buttons themselves (the same precedent as `RepzTopAppBar`'s
 * `backButtonModifier`), so a test tag lands on the tappable target instead of the whole dialog.
 * M3's own dialog enter/exit
 * (MOTION.md has no dialog-specific spec).
 */
@Composable
fun RepzAlertDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    confirmButtonModifier: Modifier = Modifier,
    dismissButtonModifier: Modifier = Modifier,
    isConfirmDestructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = confirmButtonModifier,
                colors =
                    if (isConfirmDestructive) {
                        ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    } else {
                        ButtonDefaults.textButtonColors()
                    },
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest, modifier = dismissButtonModifier) {
                Text(dismissText)
            }
        },
        title = {
            RepzDialogScrimEffect(DIALOG_SCRIM_OPACITY)
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        properties = repzDialogProperties(DIALOG_SCRIM_OPACITY),
    )
}
