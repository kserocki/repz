package com.repz.app.feature.workout.impl.presentation.live

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.repz.app.core.designsystem.component.workout.RepzLiveWorkoutHeader
import com.repz.app.core.designsystem.component.workout.RepzMenuItem
import com.repz.app.feature.workout.impl.generated.resources.Res
import com.repz.app.feature.workout.impl.generated.resources.live_discard
import com.repz.app.feature.workout.impl.generated.resources.live_finish
import com.repz.app.feature.workout.impl.generated.resources.live_minimize
import com.repz.app.feature.workout.impl.generated.resources.live_more_options
import com.repz.app.feature.workout.impl.generated.resources.live_name_edit
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource

/**
 * Maps the live workout's header fields onto [RepzLiveWorkoutHeader] (DESIGN_SYSTEM §9, board
 * 09/14): the overflow menu is just "Discard workout" for now. TODO(REP-22): swap it for the
 * Filled-emphasis destructive confirm once [LiveWorkoutDiscardDialog]'s dialog supports it.
 */
@Composable
internal fun LiveWorkoutHeaderSection(
    name: String,
    elapsedText: String,
    isFinishEnabled: Boolean,
    isEditingName: Boolean,
    onNameClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onNameEditingFinish: () -> Unit,
    onMinimizeClick: () -> Unit,
    onFinishClick: () -> Unit,
    onDiscardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RepzLiveWorkoutHeader(
        name = name,
        elapsedText = elapsedText,
        isFinishEnabled = isFinishEnabled,
        finishLabel = stringResource(Res.string.live_finish),
        isEditingName = isEditingName,
        onNameClick = onNameClick,
        onNameChange = onNameChange,
        onNameEditingFinish = onNameEditingFinish,
        onMinimizeClick = onMinimizeClick,
        onFinishClick = onFinishClick,
        overflowItems =
            persistentListOf(
                RepzMenuItem(
                    label = stringResource(Res.string.live_discard),
                    onClick = onDiscardClick,
                    isDestructive = true,
                    modifier = Modifier.testTag("live_discard"),
                ),
            ),
        minimizeContentDescription = stringResource(Res.string.live_minimize),
        nameContentDescription = stringResource(Res.string.live_name_edit, name),
        moreOptionsContentDescription = stringResource(Res.string.live_more_options),
        modifier = modifier,
        minimizeButtonModifier = Modifier.testTag("live_minimize"),
        nameModifier = Modifier.testTag("live_name"),
        finishButtonModifier = Modifier.testTag("live_finish"),
        overflowButtonModifier = Modifier.testTag("live_overflow"),
    )
}
