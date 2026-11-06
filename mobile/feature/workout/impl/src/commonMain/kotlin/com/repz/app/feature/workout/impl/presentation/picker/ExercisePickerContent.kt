package com.repz.app.feature.workout.impl.presentation.picker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.repz.app.core.designsystem.component.RepzTopAppBar
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.ui.label.label
import com.repz.app.feature.workout.impl.generated.resources.Res
import com.repz.app.feature.workout.impl.generated.resources.picker_title
import org.jetbrains.compose.resources.stringResource

/**
 * v0: a plain, full-screen list sorted by localized name (TODO(REP-37): search, filters,
 * multi-select, create custom). Tapping a row adds that exercise and closes (the one-tap flow
 * [ExercisePickerUiState] models, not board 10's multi-select "Add (n)").
 */
@Composable
fun ExercisePickerContent(
    state: ExercisePickerUiState,
    onIntent: (ExercisePickerIntent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        RepzTopAppBar(
            title = stringResource(Res.string.picker_title),
            onBackClick = onBackClick,
            backButtonModifier = Modifier.testTag("picker_back"),
        )
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize())
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(items = state.exercises, key = { _, exercise -> exercise.id.value }) { index, exercise ->
                    PickerExerciseRow(
                        exercise = exercise,
                        onClick = { onIntent(ExercisePickerIntent.ExerciseClicked(exercise.id)) },
                        modifier = Modifier.testTag("picker_item_$index"),
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerExerciseRow(
    exercise: PickerExerciseUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = RepzSpacing.xxxl)
                .clickable(onClick = onClick)
                .semantics { role = Role.Button }
                .padding(horizontal = RepzSpacing.lg, vertical = RepzSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(RepzSpacing.xxs),
    ) {
        Text(text = exercise.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = exercise.primaryMuscle.label(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
