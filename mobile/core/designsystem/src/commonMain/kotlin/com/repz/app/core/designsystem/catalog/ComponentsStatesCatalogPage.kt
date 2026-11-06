package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.component.RepzButton
import com.repz.app.core.designsystem.component.RepzButtonSize
import com.repz.app.core.designsystem.component.RepzButtonStyle
import com.repz.app.core.designsystem.component.RepzEmptyState
import com.repz.app.core.designsystem.component.RepzIcon
import com.repz.app.core.designsystem.component.RepzSkeleton
import com.repz.app.core.designsystem.theme.RepzIconSize
import com.repz.app.core.designsystem.theme.RepzSpacing
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

private const val SKELETON_SAMPLE_ROW_COUNT = 4
private val SkeletonSampleRowHeight = 56.dp
private val SkeletonRemountInterval = 3.seconds

/**
 * `components-states` (DESIGN_SYSTEM §9 `RepzEmptyState`/`RepzSkeleton`): an empty state with an
 * icon placeholder in the backdrop, and a 4-row skeleton re-mounted every 3 s so the 150 ms delay is
 * visible.
 */
@Composable
internal fun ComponentsStatesCatalogPage() {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.xl)) {
        EmptyStateSection()
        SkeletonSection()
    }
}

@Composable
private fun EmptyStateSection() {
    val emptyStateTitle = "No templates yet"
    val emptyStateBody = "Save a workout as a template to start it in one tap next time."
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        CatalogSectionLabel("Empty state")
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
}

@Composable
private fun SkeletonSection() {
    val skeletonDescription = "Loading"
    var remountKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(SkeletonRemountInterval)
            remountKey++
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        CatalogSectionLabel("Skeleton")
        key(remountKey) {
            RepzSkeleton(contentDescription = skeletonDescription) {
                Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
                    repeat(SKELETON_SAMPLE_ROW_COUNT) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(SkeletonSampleRowHeight)
                                    .skeletonBone(MaterialTheme.shapes.medium),
                        )
                    }
                }
            }
        }
    }
}
