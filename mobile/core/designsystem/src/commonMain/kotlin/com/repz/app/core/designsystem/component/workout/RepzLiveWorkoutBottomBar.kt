package com.repz.app.core.designsystem.component.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.component.RepzIcon
import com.repz.app.core.designsystem.theme.RepzIconSize
import com.repz.app.core.designsystem.theme.RepzShapes
import com.repz.app.core.designsystem.theme.RepzSpacing

// Board 09 bottom bar: surface background, 10/12/8 dp (top/horizontal/bottom) padding;
// component-internal, not on the §6 spacing scale.
private val BottomBarTopPadding = 10.dp
private val BottomBarHorizontalPadding = 12.dp
private val BottomBarBottomPadding = 8.dp

// Board 09's "Add exercise" pill: 56 dp secondaryContainer, end-aligned.
private val AddExerciseButtonHeight = 56.dp

// DESIGN_SYSTEM §9 "disabled: onSurface 12% container / 38% content" (board 04), same rule RepzButton applies.
private const val DISABLED_CONTAINER_ALPHA = 0.12f
private const val DISABLED_CONTENT_ALPHA = 0.38f

/**
 * The live workout's bottom bar (DESIGN_SYSTEM §9, board 09): a `surface` bar, navigation-bar
 * insets, a leading slot reserved for a future rest pill (this component doesn't draw one itself)
 * and an end-aligned, 56 dp `secondaryContainer` [addExerciseLabel] pill. [addExerciseButtonModifier]
 * tags that button individually, since [modifier] lands on the whole bar.
 */
@Composable
fun RepzLiveWorkoutBottomBar(
    addExerciseLabel: String,
    isAddExerciseEnabled: Boolean,
    onAddExerciseClick: () -> Unit,
    modifier: Modifier = Modifier,
    addExerciseButtonModifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .padding(
                    start = BottomBarHorizontalPadding,
                    top = BottomBarTopPadding,
                    end = BottomBarHorizontalPadding,
                    bottom = BottomBarBottomPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm),
    ) {
        Spacer(modifier = Modifier.weight(1f))
        AddExerciseButton(
            label = addExerciseLabel,
            isEnabled = isAddExerciseEnabled,
            onClick = onAddExerciseClick,
            modifier = addExerciseButtonModifier,
        )
    }
}

@Composable
private fun AddExerciseButton(
    label: String,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor =
        if (isEnabled) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTAINER_ALPHA)
        }
    val contentColor =
        if (isEnabled) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA)
        }

    Row(
        modifier =
            modifier
                .heightIn(min = AddExerciseButtonHeight)
                .clip(RepzShapes.full)
                .background(containerColor)
                .clickable(enabled = isEnabled, onClick = onClick)
                .padding(horizontal = RepzSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RepzSpacing.xs),
    ) {
        RepzIcon(icon = RepzIcon.Add, contentDescription = null, tint = contentColor, modifier = Modifier.size(RepzIconSize.dense))
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = contentColor)
    }
}
