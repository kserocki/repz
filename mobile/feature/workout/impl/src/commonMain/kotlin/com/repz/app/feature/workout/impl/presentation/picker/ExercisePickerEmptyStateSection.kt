package com.repz.app.feature.workout.impl.presentation.picker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.repz.app.core.designsystem.component.RepzButton
import com.repz.app.core.designsystem.component.RepzButtonSize
import com.repz.app.core.designsystem.component.RepzButtonStyle
import com.repz.app.core.designsystem.component.RepzEmptyState
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.feature.workout.impl.generated.resources.Res
import com.repz.app.feature.workout.impl.generated.resources.picker_clear_filters
import com.repz.app.feature.workout.impl.generated.resources.picker_no_exercises_body
import com.repz.app.feature.workout.impl.generated.resources.picker_no_exercises_title
import com.repz.app.feature.workout.impl.generated.resources.picker_no_filter_match_title
import com.repz.app.feature.workout.impl.generated.resources.picker_no_match_body
import com.repz.app.feature.workout.impl.generated.resources.picker_no_match_title
import org.jetbrains.compose.resources.stringResource

/** The picker's empty states (DESIGN_SYSTEM §9): a vertically scrollable, centred
 * [RepzEmptyState] instead of the list, so +30 %/PL/200 % font never clips. */
@Composable
internal fun ExercisePickerEmptyStateSection(
    emptyState: PickerEmptyState,
    onClearFiltersClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(RepzSpacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        when (emptyState) {
            PickerEmptyState.NoExercises -> {
                RepzEmptyState(
                    title = stringResource(Res.string.picker_no_exercises_title),
                    body = stringResource(Res.string.picker_no_exercises_body),
                    illustration = null,
                    modifier = Modifier.testTag("picker_empty"),
                )
            }

            is PickerEmptyState.NoMatchForQuery -> {
                RepzEmptyState(
                    title = stringResource(Res.string.picker_no_match_title, emptyState.query),
                    body = stringResource(Res.string.picker_no_match_body),
                    illustration = null,
                    modifier = Modifier.testTag("picker_empty"),
                ) {
                    if (emptyState.activeFilterCount > 0) {
                        ClearFiltersButton(count = emptyState.activeFilterCount, onClick = onClearFiltersClick)
                    }
                }
            }

            is PickerEmptyState.NoMatchForFilters -> {
                RepzEmptyState(
                    title = stringResource(Res.string.picker_no_filter_match_title),
                    body = null,
                    illustration = null,
                    modifier = Modifier.testTag("picker_empty"),
                ) {
                    ClearFiltersButton(count = emptyState.activeFilterCount, onClick = onClearFiltersClick)
                }
            }
        }
    }
}

@Composable
private fun ClearFiltersButton(
    count: Int,
    onClick: () -> Unit,
) {
    RepzButton(
        text = stringResource(Res.string.picker_clear_filters, count),
        onClick = onClick,
        style = RepzButtonStyle.Tonal,
        size = RepzButtonSize.Medium,
        modifier = Modifier.testTag("picker_clear_filters"),
    )
}
