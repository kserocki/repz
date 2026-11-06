package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.repz.app.core.designsystem.component.RepzAlertDialog
import com.repz.app.core.designsystem.component.RepzBanner
import com.repz.app.core.designsystem.component.RepzBannerStyle
import com.repz.app.core.designsystem.component.RepzButton
import com.repz.app.core.designsystem.component.RepzButtonSize
import com.repz.app.core.designsystem.component.RepzButtonStyle
import com.repz.app.core.designsystem.component.RepzDialogConfirmEmphasis
import com.repz.app.core.designsystem.component.RepzIcon
import com.repz.app.core.designsystem.component.RepzLoadingIndicator
import com.repz.app.core.designsystem.component.RepzSnackbarHost
import com.repz.app.core.designsystem.component.RepzSnackbarHostState
import com.repz.app.core.designsystem.component.rememberRepzSnackbarHostState
import com.repz.app.core.designsystem.theme.RepzSpacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * `components-feedback` (DESIGN_SYSTEM §9 `RepzBanner`/`RepzLoadingIndicator`/`RepzAlertDialog`/
 * `RepzSnackbarHostState`): both banner styles, plain + contained loading indicators, buttons opening
 * the two dialog variants, and buttons for the undo and message snackbars.
 */
@Composable
internal fun ComponentsFeedbackCatalogPage() {
    val snackbarHostState = rememberRepzSnackbarHostState()
    var isTextDialogVisible by remember { mutableStateOf(false) }
    var isFilledDialogVisible by remember { mutableStateOf(false) }

    Box {
        Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
            FeedbackBannersSection()
            FeedbackLoadingIndicatorsSection()
            FeedbackDialogTriggersSection(
                onShowTextDialog = { isTextDialogVisible = true },
                onShowFilledDialog = { isFilledDialogVisible = true },
            )
            FeedbackSnackbarTriggersSection(snackbarHostState)
        }

        RepzSnackbarHost(
            state = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        )
    }

    if (isTextDialogVisible) {
        FeedbackTextDialog(onDismiss = { isTextDialogVisible = false })
    }
    if (isFilledDialogVisible) {
        FeedbackFilledDestructiveDialog(onDismiss = { isFilledDialogVisible = false })
    }
}

@Composable
private fun FeedbackBannersSection() {
    val offlineBannerTitle = "You're offline"
    val offlineBannerBody = "Logging in needs a connection. Once you're in, Repz works offline."
    val neutralBannerTitle = "Offline — everything still works. We'll sync later."
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        CatalogSectionLabel("Banners")
        RepzBanner(icon = RepzIcon.CloudOff, title = offlineBannerTitle, body = offlineBannerBody)
        RepzBanner(icon = RepzIcon.CloudOff, title = neutralBannerTitle, style = RepzBannerStyle.Neutral)
    }
}

@Composable
private fun FeedbackLoadingIndicatorsSection() {
    val loadingDescription = "Loading"
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        CatalogSectionLabel("Loading indicators")
        Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
            RepzLoadingIndicator(contentDescription = loadingDescription)
            RepzLoadingIndicator(contentDescription = loadingDescription, contained = true)
        }
    }
}

@Composable
private fun FeedbackDialogTriggersSection(
    onShowTextDialog: () -> Unit,
    onShowFilledDialog: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        CatalogSectionLabel("Dialogs")
        Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
            RepzButton("Text confirm", onClick = onShowTextDialog, style = RepzButtonStyle.Outlined, size = RepzButtonSize.Small)
            RepzButton(
                "Filled destructive",
                onClick = onShowFilledDialog,
                style = RepzButtonStyle.Outlined,
                size = RepzButtonSize.Small,
            )
        }
    }
}

@Composable
private fun FeedbackSnackbarTriggersSection(snackbarHostState: RepzSnackbarHostState) {
    val coroutineScope = rememberCoroutineScope()
    var lastUndoResult by remember { mutableStateOf<Boolean?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        CatalogSectionLabel("Snackbars")
        Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
            RepzButton(
                "Undo snackbar",
                onClick = {
                    coroutineScope.launch { lastUndoResult = snackbarHostState.showUndo("Set deleted", "Undo") }
                },
                style = RepzButtonStyle.Outlined,
                size = RepzButtonSize.Small,
            )
            RepzButton(
                "Message snackbar",
                onClick = { coroutineScope.showSyncErrorMessage(snackbarHostState) },
                style = RepzButtonStyle.Outlined,
                size = RepzButtonSize.Small,
            )
        }
        val undoResult = lastUndoResult
        if (undoResult != null) {
            val undoResultLabel = "Undo tapped: $undoResult"
            Text(undoResultLabel, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun CoroutineScope.showSyncErrorMessage(snackbarHostState: RepzSnackbarHostState) {
    launch { snackbarHostState.showMessage("Couldn't sync. Check connection.") }
}

@Composable
private fun FeedbackTextDialog(onDismiss: () -> Unit) {
    val dialogTitle = "Discard changes?"
    val dialogText = "You'll lose anything you typed on this screen."
    RepzAlertDialog(
        title = dialogTitle,
        text = dialogText,
        confirmText = "Discard",
        onConfirm = onDismiss,
        dismissText = "Keep editing",
        onDismissRequest = onDismiss,
    )
}

@Composable
private fun FeedbackFilledDestructiveDialog(onDismiss: () -> Unit) {
    val dialogTitle = "Discard workout"
    val dialogText = "Push A has 3 logged sets. Discarding removes them from this device. This can't be undone."
    RepzAlertDialog(
        title = dialogTitle,
        text = dialogText,
        confirmText = "Discard",
        onConfirm = onDismiss,
        dismissText = "Keep training",
        onDismissRequest = onDismiss,
        icon = RepzIcon.Error,
        isConfirmDestructive = true,
        confirmEmphasis = RepzDialogConfirmEmphasis.Filled,
    )
}
