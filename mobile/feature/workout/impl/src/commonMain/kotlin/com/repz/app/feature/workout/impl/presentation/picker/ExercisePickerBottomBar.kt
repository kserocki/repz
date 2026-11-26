package com.repz.app.feature.workout.impl.presentation.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.repz.app.core.designsystem.component.RepzButton
import com.repz.app.core.designsystem.component.RepzButtonSize
import com.repz.app.core.designsystem.component.RepzButtonStyle
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.feature.workout.impl.generated.resources.Res
import com.repz.app.feature.workout.impl.generated.resources.picker_add_selected
import com.repz.app.feature.workout.impl.generated.resources.picker_selected_count
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The Add-mode bottom bar (DESIGN_SYSTEM §9, board 10 C13): shown only while [selectedCount] > 0,
 * appearing/disappearing instantly (no motion in this issue). `windowInsetsPadding` takes the
 * navigation-bar inset `union`ed with the IME inset (the larger of the two, not their sum) so the
 * bar sits just above an open keyboard without double-padding when it's closed.
 */
@Composable
internal fun ExercisePickerBottomBar(
    selectedCount: Int,
    isSubmitting: Boolean,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(horizontal = RepzSpacing.lg, vertical = RepzSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RepzSpacing.md),
    ) {
        // TODO(REP-49): MOT-09 roll on the count; the swap is instant here.
        Text(
            text = pluralStringResource(Res.plurals.picker_selected_count, selectedCount, selectedCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
                Modifier
                    .weight(1f)
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag("picker_selected_count"),
        )
        RepzButton(
            text = stringResource(Res.string.picker_add_selected, selectedCount),
            onClick = onAddClick,
            style = RepzButtonStyle.Hero,
            size = RepzButtonSize.Medium,
            enabled = !isSubmitting,
            modifier = Modifier.testTag("picker_add"),
        )
    }
}
