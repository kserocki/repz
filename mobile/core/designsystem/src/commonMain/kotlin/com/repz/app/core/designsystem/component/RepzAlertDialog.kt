package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.repz.app.core.designsystem.theme.RepzIconSize

// DESIGN_SYSTEM §7: sheets and dialogs use a scrim of `#000` at 32 %, over the M3 `scrim` role
// (`#000000` in both themes) instead of a hard-coded black.
private const val DIALOG_SCRIM_OPACITY = 0.32f

/**
 * U3: the confirm button's weight. [RepzDialogConfirmEmphasis.Text] is the existing low-emphasis
 * button (unchanged since the Log out dialog's own decision, DESIGN_SYSTEM §13, the default);
 * [RepzDialogConfirmEmphasis.Filled] is board 06's pill button, for an irreversible action such as
 * discarding a workout.
 */
enum class RepzDialogConfirmEmphasis { Text, Filled }

/**
 * A confirmation dialog (DESIGN_SYSTEM §9, boards 06/25): M3 `AlertDialog` (ADR-0006) in the
 * `surfaceContainerHigh` container, `shapes.extraLarge` (28 dp), an optional centred [icon] (24 dp,
 * `error` tinted when [isConfirmDestructive] else `secondary`, board 06), centred `headlineSmall`
 * [title] and centred `bodyMedium`/`onSurfaceVariant` [text]. [dismissText] always renders as a plain
 * text button (e.g. "Cancel", `primary`, M3's default text-button colour); [confirmEmphasis] (U3)
 * picks [confirmText]'s own weight: [RepzDialogConfirmEmphasis.Text] (the default, `error` when
 * [isConfirmDestructive], matching the F25 Log out row) or [RepzDialogConfirmEmphasis.Filled] (a pill
 * button, `error`/`onError` when [isConfirmDestructive], `primary`/`onPrimary` otherwise).
 * [onDismissRequest] also fires on Back and a scrim tap, in addition to [dismissText]'s own click —
 * callers wire both to the same "cancel" handling. [confirmButtonModifier] and
 * [dismissButtonModifier] tag the buttons themselves (the same precedent as `RepzTopAppBar`'s
 * `backButtonModifier`), so a test tag lands on the tappable target instead of the whole dialog.
 * Enter/exit motion is M3's own dialog default; MOTION.md has no dialog-specific spec.
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
    icon: RepzIcon? = null,
    confirmEmphasis: RepzDialogConfirmEmphasis = RepzDialogConfirmEmphasis.Text,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        confirmButton = {
            DialogConfirmButton(
                confirmText = confirmText,
                onConfirm = onConfirm,
                modifier = confirmButtonModifier,
                isConfirmDestructive = isConfirmDestructive,
                confirmEmphasis = confirmEmphasis,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest, modifier = dismissButtonModifier) {
                Text(dismissText)
            }
        },
        icon =
            icon?.let { dialogIcon ->
                {
                    RepzIcon(icon = dialogIcon, contentDescription = null, modifier = Modifier.size(RepzIconSize.default))
                }
            },
        iconContentColor = if (isConfirmDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
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

@Composable
private fun DialogConfirmButton(
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    isConfirmDestructive: Boolean = false,
    confirmEmphasis: RepzDialogConfirmEmphasis = RepzDialogConfirmEmphasis.Text,
) {
    when (confirmEmphasis) {
        RepzDialogConfirmEmphasis.Text -> {
            TextButton(
                onClick = onConfirm,
                modifier = modifier,
                colors =
                    if (isConfirmDestructive) {
                        ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    } else {
                        ButtonDefaults.textButtonColors()
                    },
            ) {
                Text(confirmText)
            }
        }

        RepzDialogConfirmEmphasis.Filled -> {
            Button(
                onClick = onConfirm,
                modifier = modifier,
                colors =
                    if (isConfirmDestructive) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
            ) {
                Text(confirmText)
            }
        }
    }
}
