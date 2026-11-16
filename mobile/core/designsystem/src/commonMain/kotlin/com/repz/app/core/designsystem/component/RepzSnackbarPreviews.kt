package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

private val PreviewSnackbarMinHeight = 48.dp

/** The slot-based [Snackbar] (not [RepzSnackbarHostState], which needs a live coroutine scope)
 * previews the same `shapes.small` / inverse-surface / `labelLargeEmphasized`+`inversePrimary`
 * action styling [RepzSnackbarHost] renders (board 06). */
@Composable
private fun RepzSnackbarShowcase() {
    val message = "Something went wrong. Try again"
    val retryLabel = "Retry"
    val undoMessage = "Set deleted"
    val undoLabel = "Undo"
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.md)) {
        Snackbar(modifier = Modifier.heightIn(min = PreviewSnackbarMinHeight), shape = MaterialTheme.shapes.small) {
            Text(message)
        }
        RepzSnackbarActionSample(message, retryLabel)
        RepzSnackbarActionSample(undoMessage, undoLabel)
    }
}

@Composable
private fun RepzSnackbarActionSample(
    message: String,
    actionLabel: String,
) {
    Snackbar(
        modifier = Modifier.heightIn(min = PreviewSnackbarMinHeight),
        shape = MaterialTheme.shapes.small,
        action = {
            TextButton(
                onClick = {},
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.inversePrimary),
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelLargeEmphasized)
            }
        },
    ) {
        Text(message)
    }
}

@Preview
@Composable
private fun RepzSnackbarLightPreview() {
    RepzTheme(darkTheme = false) { RepzSnackbarShowcase() }
}

@Preview
@Composable
private fun RepzSnackbarDarkPreview() {
    RepzTheme(darkTheme = true) { RepzSnackbarShowcase() }
}
