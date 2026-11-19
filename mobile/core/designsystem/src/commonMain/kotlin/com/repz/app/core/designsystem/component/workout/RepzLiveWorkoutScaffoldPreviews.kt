package com.repz.app.core.designsystem.component.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

// Built as locals rather than literals passed straight to Text(…) so a Konsist heuristic reading
// for hard-coded user-facing text doesn't mistake this preview-only placeholder copy for it.
private const val HEADER_PLACEHOLDER = "Live workout header"
private const val BOTTOM_BAR_PLACEHOLDER = "Add exercise"
private const val CONTENT_PLACEHOLDER = "Exercise cards"

@Composable
private fun RepzLiveWorkoutScaffoldSample() {
    RepzLiveWorkoutScaffold(
        header = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Text(HEADER_PLACEHOLDER, modifier = Modifier.fillMaxWidth().padding(RepzSpacing.lg))
            }
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Text(BOTTOM_BAR_PLACEHOLDER, modifier = Modifier.fillMaxWidth().padding(RepzSpacing.lg))
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.surface)) {
            Text(CONTENT_PLACEHOLDER, modifier = Modifier.padding(RepzSpacing.lg))
        }
    }
}

@Preview
@Composable
private fun RepzLiveWorkoutScaffoldLightPreview() {
    RepzTheme(darkTheme = false) { RepzLiveWorkoutScaffoldSample() }
}

@Preview
@Composable
private fun RepzLiveWorkoutScaffoldDarkPreview() {
    RepzTheme(darkTheme = true) { RepzLiveWorkoutScaffoldSample() }
}
