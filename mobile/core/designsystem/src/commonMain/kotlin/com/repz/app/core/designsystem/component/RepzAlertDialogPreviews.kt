package com.repz.app.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzTheme

@Composable
private fun RepzAlertDialogSample() {
    val dialogTitle = "Discard changes?"
    val dialogText = "You'll lose anything you typed on this screen."
    RepzAlertDialog(
        title = dialogTitle,
        text = dialogText,
        confirmText = "Discard",
        onConfirm = {},
        dismissText = "Keep editing",
        onDismissRequest = {},
    )
}

@Composable
private fun RepzAlertDialogDestructiveSample() {
    val dialogTitle = "Log out?"
    val dialogText = "You can log back in any time. Your data stays in your account."
    RepzAlertDialog(
        title = dialogTitle,
        text = dialogText,
        confirmText = "Log out",
        onConfirm = {},
        dismissText = "Cancel",
        onDismissRequest = {},
        isConfirmDestructive = true,
    )
}

@Composable
private fun RepzAlertDialogLongPlSample() {
    val dialogTitle = "Wylogować się?"
    val dialogText = "W każdej chwili możesz zalogować się ponownie. Twoje dane zostają na Twoim koncie."
    RepzAlertDialog(
        title = dialogTitle,
        text = dialogText,
        confirmText = "Wyloguj się",
        onConfirm = {},
        dismissText = "Anuluj",
        onDismissRequest = {},
        isConfirmDestructive = true,
    )
}

@Preview
@Composable
private fun RepzAlertDialogLightPreview() {
    RepzTheme(darkTheme = false) { RepzAlertDialogSample() }
}

@Preview
@Composable
private fun RepzAlertDialogDarkPreview() {
    RepzTheme(darkTheme = true) { RepzAlertDialogSample() }
}

@Preview
@Composable
private fun RepzAlertDialogDestructiveLightPreview() {
    RepzTheme(darkTheme = false) { RepzAlertDialogDestructiveSample() }
}

@Preview
@Composable
private fun RepzAlertDialogDestructiveDarkPreview() {
    RepzTheme(darkTheme = true) { RepzAlertDialogDestructiveSample() }
}

@Preview
@Composable
private fun RepzAlertDialogLongPlLightPreview() {
    RepzTheme(darkTheme = false) { RepzAlertDialogLongPlSample() }
}

@Preview
@Composable
private fun RepzAlertDialogLongPlDarkPreview() {
    RepzTheme(darkTheme = true) { RepzAlertDialogLongPlSample() }
}
