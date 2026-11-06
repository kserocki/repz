package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzIconSize
import com.repz.app.core.designsystem.theme.RepzTheme

@Composable
private fun RepzEmptyStateSample() {
    val emptyStateTitle = "No templates yet"
    val emptyStateBody = "Save a workout as a template to start it in one tap next time."
    RepzEmptyState(
        title = emptyStateTitle,
        body = emptyStateBody,
        illustration = {
            RepzIcon(
                icon = RepzIcon.FitnessCenter,
                contentDescription = null,
                modifier = Modifier.size(RepzIconSize.illustration),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = Modifier.fillMaxWidth(),
        actions = {
            RepzButton("Create template", onClick = {}, style = RepzButtonStyle.Filled, size = RepzButtonSize.Small)
        },
    )
}

@Preview
@Composable
private fun RepzEmptyStateLightPreview() {
    RepzTheme(darkTheme = false) { RepzEmptyStateSample() }
}

@Preview
@Composable
private fun RepzEmptyStateDarkPreview() {
    RepzTheme(darkTheme = true) { RepzEmptyStateSample() }
}
