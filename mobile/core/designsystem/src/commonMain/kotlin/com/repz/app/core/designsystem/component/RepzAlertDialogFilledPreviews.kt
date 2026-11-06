package com.repz.app.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzTheme

// Split out of RepzAlertDialogPreviews.kt (same component, DESIGN_SYSTEM §9 RepzAlertDialog) to stay
// under Detekt's per-file function-count limit.

@Composable
private fun RepzAlertDialogFilledDestructiveSample() {
    val dialogTitle = "Discard workout"
    val dialogText = "Push A has 3 logged sets. Discarding removes them from this device. This can't be undone."
    RepzAlertDialog(
        title = dialogTitle,
        text = dialogText,
        confirmText = "Discard",
        onConfirm = {},
        dismissText = "Keep training",
        onDismissRequest = {},
        isConfirmDestructive = true,
        icon = RepzIcon.Error,
        confirmEmphasis = RepzDialogConfirmEmphasis.Filled,
    )
}

@Composable
private fun RepzAlertDialogFilledSample() {
    val dialogTitle = "Save as template?"
    val dialogText = "You can start this workout in one tap next time."
    RepzAlertDialog(
        title = dialogTitle,
        text = dialogText,
        confirmText = "Save",
        onConfirm = {},
        dismissText = "Not now",
        onDismissRequest = {},
        icon = RepzIcon.Check,
        confirmEmphasis = RepzDialogConfirmEmphasis.Filled,
    )
}

@Preview
@Composable
private fun RepzAlertDialogFilledDestructiveLightPreview() {
    RepzTheme(darkTheme = false) { RepzAlertDialogFilledDestructiveSample() }
}

@Preview
@Composable
private fun RepzAlertDialogFilledDestructiveDarkPreview() {
    RepzTheme(darkTheme = true) { RepzAlertDialogFilledDestructiveSample() }
}

@Preview
@Composable
private fun RepzAlertDialogFilledLightPreview() {
    RepzTheme(darkTheme = false) { RepzAlertDialogFilledSample() }
}

@Preview
@Composable
private fun RepzAlertDialogFilledDarkPreview() {
    RepzTheme(darkTheme = true) { RepzAlertDialogFilledSample() }
}
