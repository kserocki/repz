package com.repz.app.feature.workout.impl.presentation.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import com.repz.app.core.designsystem.component.RepzButton
import com.repz.app.core.designsystem.component.RepzButtonSize
import com.repz.app.core.designsystem.component.RepzButtonStyle
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.domain.model.SetType
import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.model.WorkoutExerciseId
import com.repz.app.core.domain.model.WorkoutSetId
import com.repz.app.feature.workout.impl.generated.resources.Res
import com.repz.app.feature.workout.impl.generated.resources.live_empty_add_exercises
import com.repz.app.feature.workout.impl.generated.resources.live_empty_body
import com.repz.app.feature.workout.impl.generated.resources.live_empty_title
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource

/**
 * The live workout's card list (DESIGN_SYSTEM §9, board 09): 16 dp margins, 12 dp gaps between
 * cards; [isEmpty] shows the empty state (board 09 §10a) instead.
 * TODO(REP-22): use the generic empty-state component once it lands.
 */
@Composable
internal fun LiveWorkoutExerciseList(
    isEmpty: Boolean,
    exercises: ImmutableList<LiveExerciseUi>,
    unit: UnitSystem,
    editingField: SetFieldRef?,
    onAddExercisesClick: () -> Unit,
    onReorderClick: () -> Unit,
    onNotesClick: (WorkoutExerciseId) -> Unit,
    onNotesChange: (WorkoutExerciseId, String) -> Unit,
    onNotesEditingFinish: (WorkoutExerciseId) -> Unit,
    onRemoveClick: (WorkoutExerciseId) -> Unit,
    onAddSetClick: (WorkoutExerciseId) -> Unit,
    onSetFieldClick: (WorkoutSetId, SetField) -> Unit,
    onSetCompleteClick: (WorkoutSetId) -> Unit,
    onSetDeleteClick: (WorkoutSetId) -> Unit,
    onSetTypeSelect: (WorkoutSetId, SetType) -> Unit,
    listState: LazyListState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    if (isEmpty) {
        LiveWorkoutEmptyState(onAddExercisesClick = onAddExercisesClick, modifier = modifier.fillMaxSize().padding(contentPadding))
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(RepzSpacing.md),
    ) {
        // Keyed by the exercise's own id (stable across reorder); indexed for the "card_$index"
        // test tag each card carries (see LiveWorkoutExerciseCard).
        itemsIndexed(items = exercises, key = { _, exercise -> exercise.id.value }) { index, exercise ->
            LiveWorkoutExerciseCard(
                index = index,
                exercise = exercise,
                unit = unit,
                editingField = editingField,
                onReorderClick = onReorderClick,
                onNotesClick = { onNotesClick(exercise.id) },
                onNotesChange = { notes -> onNotesChange(exercise.id, notes) },
                onNotesEditingFinish = { onNotesEditingFinish(exercise.id) },
                onRemoveClick = { onRemoveClick(exercise.id) },
                onAddSetClick = { onAddSetClick(exercise.id) },
                onSetFieldClick = onSetFieldClick,
                onSetCompleteClick = onSetCompleteClick,
                onSetDeleteClick = onSetDeleteClick,
                onSetTypeSelect = onSetTypeSelect,
            )
        }
    }
}

@Composable
private fun LiveWorkoutEmptyState(
    onAddExercisesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(RepzSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(RepzSpacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.live_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.live_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        RepzButton(
            text = stringResource(Res.string.live_empty_add_exercises),
            onClick = onAddExercisesClick,
            modifier = Modifier.testTag("live_empty_add_exercises"),
            style = RepzButtonStyle.Filled,
            size = RepzButtonSize.Medium,
        )
    }
}
