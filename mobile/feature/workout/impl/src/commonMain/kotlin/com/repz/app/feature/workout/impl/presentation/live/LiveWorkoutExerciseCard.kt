package com.repz.app.feature.workout.impl.presentation.live

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.repz.app.core.designsystem.component.workout.RepzExerciseCard
import com.repz.app.core.designsystem.component.workout.RepzMenuItem
import com.repz.app.core.designsystem.component.workout.RepzSetColumnLabels
import com.repz.app.core.designsystem.component.workout.RepzSetRow
import com.repz.app.core.domain.model.SetType
import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.model.WorkoutSetId
import com.repz.app.core.ui.format.formatSetWeight
import com.repz.app.core.ui.format.weightUnitLabel
import com.repz.app.core.ui.label.label
import com.repz.app.feature.workout.impl.generated.resources.Res
import com.repz.app.feature.workout.impl.generated.resources.card_add_set
import com.repz.app.feature.workout.impl.generated.resources.card_column_previous
import com.repz.app.feature.workout.impl.generated.resources.card_column_reps
import com.repz.app.feature.workout.impl.generated.resources.card_column_set
import com.repz.app.feature.workout.impl.generated.resources.card_menu_notes
import com.repz.app.feature.workout.impl.generated.resources.card_menu_remove
import com.repz.app.feature.workout.impl.generated.resources.card_menu_reorder
import com.repz.app.feature.workout.impl.generated.resources.card_notes_label
import com.repz.app.feature.workout.impl.generated.resources.card_options
import com.repz.app.feature.workout.impl.generated.resources.card_progress
import com.repz.app.feature.workout.impl.generated.resources.card_subtitle
import com.repz.app.feature.workout.impl.generated.resources.exercise_unknown
import com.repz.app.feature.workout.impl.generated.resources.set_badge
import com.repz.app.feature.workout.impl.generated.resources.set_previous_none
import com.repz.app.feature.workout.impl.generated.resources.set_reps
import com.repz.app.feature.workout.impl.generated.resources.set_weight
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * One [LiveExerciseUi] card (DESIGN_SYSTEM §9 `ExerciseCard`, board 05/09): maps the UI model onto
 * [RepzExerciseCard], keyed by [index] for its test tags (§3.10). Only `*Screen`/`*Content` may know
 * [LiveWorkoutIntent] (AGENTS.md §4, Konsist N8), so every callback here is explicit and present
 * tense; [LiveWorkoutExerciseList] binds [exercise]'s own id into them before calling this.
 */
@Composable
internal fun LiveWorkoutExerciseCard(
    index: Int,
    exercise: LiveExerciseUi,
    unit: UnitSystem,
    editingField: SetFieldRef?,
    onReorderClick: () -> Unit,
    onNotesClick: () -> Unit,
    onNotesChange: (String) -> Unit,
    onNotesEditingFinish: () -> Unit,
    onRemoveClick: () -> Unit,
    onAddSetClick: () -> Unit,
    onSetFieldClick: (WorkoutSetId, SetField) -> Unit,
    onSetCompleteClick: (WorkoutSetId) -> Unit,
    onSetDeleteClick: (WorkoutSetId) -> Unit,
    onSetTypeSelect: (WorkoutSetId, SetType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = exercise.name ?: stringResource(Res.string.exercise_unknown)
    val progress =
        pluralStringResource(Res.plurals.card_progress, exercise.completedSetCount, exercise.completedSetCount, exercise.sets.size)
    val muscleLabel = exercise.primaryMuscle?.label()
    val subtitle = if (muscleLabel != null) stringResource(Res.string.card_subtitle, muscleLabel, progress) else progress
    val notes = if (exercise.isEditingNotes || exercise.notes.isNotEmpty()) exercise.notes else null

    RepzExerciseCard(
        name = name,
        subtitle = subtitle,
        notes = notes,
        isEditingNotes = exercise.isEditingNotes,
        notesLabel = stringResource(Res.string.card_notes_label),
        onNotesClick = onNotesClick,
        onNotesChange = onNotesChange,
        onNotesEditingFinish = onNotesEditingFinish,
        menuItems = cardMenuItems(onReorderClick, onNotesClick, onRemoveClick),
        menuContentDescription = stringResource(Res.string.card_options),
        onHeaderLongClick = onReorderClick,
        columnLabels =
            RepzSetColumnLabels(
                set = stringResource(Res.string.card_column_set),
                previous = stringResource(Res.string.card_column_previous),
                weight = weightUnitLabel(unit),
                reps = stringResource(Res.string.card_column_reps),
            ),
        addSetLabel = stringResource(Res.string.card_add_set),
        isAddSetEnabled = exercise.canAddSet,
        onAddSetClick = onAddSetClick,
        modifier = modifier.testTag("card_$index"),
        menuButtonModifier = Modifier.testTag("card_${index}_menu"),
        notesModifier = Modifier.testTag("card_${index}_notes"),
        addSetModifier = Modifier.testTag("card_${index}_add_set"),
    ) {
        LiveWorkoutSetRows(
            cardIndex = index,
            sets = exercise.sets,
            unit = unit,
            editingField = editingField,
            onSetFieldClick = onSetFieldClick,
            onSetCompleteClick = onSetCompleteClick,
            onSetDeleteClick = onSetDeleteClick,
            onSetTypeSelect = onSetTypeSelect,
        )
    }
}

@Composable
private fun cardMenuItems(
    onReorderClick: () -> Unit,
    onNotesClick: () -> Unit,
    onRemoveClick: () -> Unit,
): ImmutableList<RepzMenuItem> =
    persistentListOf(
        RepzMenuItem(label = stringResource(Res.string.card_menu_reorder), onClick = onReorderClick),
        RepzMenuItem(label = stringResource(Res.string.card_menu_notes), onClick = onNotesClick),
        RepzMenuItem(label = stringResource(Res.string.card_menu_remove), onClick = onRemoveClick, isDestructive = true),
    )

@Composable
private fun ColumnScope.LiveWorkoutSetRows(
    cardIndex: Int,
    sets: ImmutableList<LiveSetUi>,
    unit: UnitSystem,
    editingField: SetFieldRef?,
    onSetFieldClick: (WorkoutSetId, SetField) -> Unit,
    onSetCompleteClick: (WorkoutSetId) -> Unit,
    onSetDeleteClick: (WorkoutSetId) -> Unit,
    onSetTypeSelect: (WorkoutSetId, SetType) -> Unit,
) {
    sets.forEachIndexed { setIndex, set ->
        key(set.id) {
            LiveWorkoutSetRow(
                cardIndex = cardIndex,
                setIndex = setIndex,
                set = set,
                unit = unit,
                editingField = if (editingField?.setId == set.id) editingField.field else null,
                onFieldClick = onSetFieldClick,
                onCompleteClick = onSetCompleteClick,
                onDeleteClick = onSetDeleteClick,
                onTypeSelect = onSetTypeSelect,
            )
        }
    }
}

/** One [LiveSetUi] row, keyed by [cardIndex]/[setIndex] for its test tags. */
@Composable
private fun LiveWorkoutSetRow(
    cardIndex: Int,
    setIndex: Int,
    set: LiveSetUi,
    unit: UnitSystem,
    editingField: SetField?,
    onFieldClick: (WorkoutSetId, SetField) -> Unit,
    onCompleteClick: (WorkoutSetId) -> Unit,
    onDeleteClick: (WorkoutSetId) -> Unit,
    onTypeSelect: (WorkoutSetId, SetType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val badgeLabel = set.badgeLabel()
    val weightText = set.weight?.let { formatSetWeight(it, unit) }.orEmpty()
    val repsText = set.reps?.toString().orEmpty()

    RepzSetRow(
        badgeLabel = badgeLabel,
        type = set.type.toRepzSetType(),
        previousText = previousText(set.previous, unit),
        noPreviousText = stringResource(Res.string.set_previous_none),
        lastLineText = lastLineText(set.previous, unit),
        weightText = weightText,
        repsText = repsText,
        isPrefilled = set.isPrefilled,
        editingField = editingField.toRepzSetField(),
        isCompleted = set.isCompleted,
        typeMenuItems = typeMenuItems(set.type) { type -> onTypeSelect(set.id, type) },
        onWeightClick = { onFieldClick(set.id, SetField.Weight) },
        onRepsClick = { onFieldClick(set.id, SetField.Reps) },
        onCompleteClick = { onCompleteClick(set.id) },
        onDelete = { onDeleteClick(set.id) },
        accessibilityActions =
            setAccessibilityActions(
                currentType = set.type,
                onDelete = { onDeleteClick(set.id) },
                onTypeSelect = { type -> onTypeSelect(set.id, type) },
            ),
        completeContentDescription = completeContentDescription(set, badgeLabel, unit),
        weightContentDescription = stringResource(Res.string.set_weight, badgeLabel),
        repsContentDescription = stringResource(Res.string.set_reps, badgeLabel),
        badgeContentDescription = stringResource(Res.string.set_badge, badgeLabel),
        modifier = modifier.testTag("set_${cardIndex}_$setIndex"),
        badgeModifier = Modifier.testTag("set_${cardIndex}_${setIndex}_badge"),
        weightModifier = Modifier.testTag("set_${cardIndex}_${setIndex}_weight"),
        repsModifier = Modifier.testTag("set_${cardIndex}_${setIndex}_reps"),
        completeModifier = Modifier.testTag("set_${cardIndex}_${setIndex}_check"),
    )
}
