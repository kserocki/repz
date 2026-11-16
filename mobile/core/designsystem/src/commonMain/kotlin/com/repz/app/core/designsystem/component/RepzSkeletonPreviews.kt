package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

private const val PREVIEW_SKELETON_ROW_COUNT = 3
private val PreviewSkeletonRowHeight = 56.dp

// Preview-only specimen text (not user-facing): AGENTS.md §4 Strings covers product copy, not a
// design-system preview's own label.
private const val PREVIEW_SKELETON_DESCRIPTION = "Loading"

@Composable
private fun RepzSkeletonRowsSample() {
    RepzSkeletonBody(contentDescription = PREVIEW_SKELETON_DESCRIPTION) {
        Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
            repeat(PREVIEW_SKELETON_ROW_COUNT) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(PreviewSkeletonRowHeight)
                            .skeletonBone(MaterialTheme.shapes.medium),
                )
            }
        }
    }
}

@Preview
@Composable
private fun RepzSkeletonLightPreview() {
    RepzTheme(darkTheme = false) { RepzSkeletonRowsSample() }
}

@Preview
@Composable
private fun RepzSkeletonDarkPreview() {
    RepzTheme(darkTheme = true) { RepzSkeletonRowsSample() }
}
