package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

/** The slot-based [Snackbar] (not [RepzSnackbarHostState], which needs a live coroutine scope)
 * previews the same `shapes.small` / inverse-surface styling [RepzSnackbarHost] renders. */
@Composable
private fun RepzSnackbarShowcase() {
    val message = "Something went wrong. Try again"
    val actionLabel = "Retry"
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.md)) {
        Snackbar(shape = MaterialTheme.shapes.small) { Text(message) }
        Snackbar(
            shape = MaterialTheme.shapes.small,
            action = { TextButton(onClick = {}) { Text(actionLabel) } },
        ) { Text(message) }
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
