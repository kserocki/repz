package com.repz.app.feature.workout.impl.presentation.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.repz.app.core.designsystem.component.RepzExerciseListItem
import com.repz.app.core.designsystem.component.RepzSkeleton
import com.repz.app.core.designsystem.theme.RepzLayout
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.feature.workout.impl.generated.resources.Res
import com.repz.app.feature.workout.impl.generated.resources.picker_section_all
import com.repz.app.feature.workout.impl.generated.resources.picker_section_recent
import com.repz.app.feature.workout.impl.generated.resources.picker_section_results
import org.jetbrains.compose.resources.stringResource

private const val LOADING_ROW_COUNT = 6

/**
 * The picker's main list (DESIGN_SYSTEM §9 picker composition): `recent` (only while
 * [ExercisePickerUiState.query] is blank), then `results` under a header that reads either
 * "All exercises" or "All results · n". [ExercisePickerContent] only calls this when
 * [ExercisePickerUiState.emptyState] is `null`; [ExercisePickerUiState.isLoading] shows
 * [LOADING_ROW_COUNT] skeleton rows instead, with no header.
 */
@Composable
internal fun ExercisePickerList(
    state: ExercisePickerUiState,
    onExerciseClick: (ExerciseId) -> Unit,
    listState: LazyListState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        ExercisePickerLoadingList(contentPadding = contentPadding, modifier = modifier)
        return
    }

    val isSelectable = state.mode == ExercisePickerMode.Add

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        if (state.recent.isNotEmpty()) {
            item(key = "header_recent") {
                ExercisePickerSectionHeader(
                    text = stringResource(Res.string.picker_section_recent),
                    modifier = Modifier.testTag("picker_header_recent"),
                )
            }
            itemsIndexed(items = state.recent, key = { _, exercise -> "recent_${exercise.id.value}" }) { index, exercise ->
                RepzExerciseListItem(
                    name = exercise.name,
                    supportingText = exercise.supportingText(),
                    onClick = { onExerciseClick(exercise.id) },
                    isSelected = if (isSelectable) exercise.isSelected else null,
                    modifier = Modifier.testTag("picker_recent_$index"),
                )
            }
        }

        item(key = "header_results") {
            ExercisePickerSectionHeader(
                text =
                    when (val header = state.resultsHeader) {
                        PickerResultsHeader.AllExercises -> stringResource(Res.string.picker_section_all)
                        is PickerResultsHeader.Results -> stringResource(Res.string.picker_section_results, header.count)
                    },
                modifier = Modifier.testTag("picker_header_results"),
            )
        }
        itemsIndexed(items = state.results, key = { _, exercise -> "result_${exercise.id.value}" }) { index, exercise ->
            RepzExerciseListItem(
                name = exercise.name,
                supportingText = exercise.supportingText(),
                onClick = { onExerciseClick(exercise.id) },
                isSelected = if (isSelectable) exercise.isSelected else null,
                modifier = Modifier.testTag("picker_result_$index"),
            )
        }

        // TODO(REP-44): "Create "<query>" as custom exercise" row (board 10 C13) goes here.
    }
}

@Composable
private fun ExercisePickerSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmallEmphasized,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = RepzSpacing.md, end = RepzSpacing.md, top = RepzSpacing.md, bottom = RepzSpacing.xs),
    )
}

@Composable
private fun ExercisePickerLoadingList(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    RepzSkeleton(modifier = modifier.fillMaxSize().padding(contentPadding)) {
        Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
            repeat(LOADING_ROW_COUNT) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(RepzLayout.listItemTwoLineHeight)
                            .skeletonBone(MaterialTheme.shapes.large),
                )
            }
        }
    }
}
