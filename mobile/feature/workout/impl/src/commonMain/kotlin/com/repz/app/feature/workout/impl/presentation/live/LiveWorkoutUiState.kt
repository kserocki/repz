package com.repz.app.feature.workout.impl.presentation.live

import com.repz.app.core.domain.model.Muscle
import com.repz.app.core.domain.model.Rpe
import com.repz.app.core.domain.model.SetType
import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.model.WorkoutExerciseId
import com.repz.app.core.domain.model.WorkoutSetId
import com.repz.app.core.domain.preferences.WeightStep
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Duration

/**
 * [keypadTarget] is derived from [editingField] (a `null` target hides the keypad and shows the
 * bottom bar, §8). [isEmpty] distinguishes the empty state (board 09 §10a) from still loading.
 */
data class LiveWorkoutUiState(
    val isLoading: Boolean = true,
    val workoutName: String = "",
    val isEditingName: Boolean = false,
    val elapsed: Duration = Duration.ZERO,
    val unit: UnitSystem = UnitSystem.KG,
    val exercises: ImmutableList<LiveExerciseUi> = persistentListOf(),
    val canFinish: Boolean = false,
    val isFinishing: Boolean = false,
    val canAddExercise: Boolean = true,
    val isReordering: Boolean = false,
    val editingField: SetFieldRef? = null,
    val keypadTarget: LiveKeypadTargetUi? = null,
    val weightStep: WeightStep = WeightStep.defaultFor(UnitSystem.KG),
    val discardConfirmation: DiscardConfirmationUi? = null,
) {
    val isEmpty: Boolean get() = !isLoading && exercises.isEmpty()
}

/** [name] is `null` when the exercise is missing even with tombstones (T9: "Unknown exercise"). */
data class LiveExerciseUi(
    val id: WorkoutExerciseId,
    val name: String?,
    val primaryMuscle: Muscle?,
    val notes: String,
    val isEditingNotes: Boolean,
    val completedSetCount: Int,
    val canAddSet: Boolean,
    val sets: ImmutableList<LiveSetUi>,
)

/**
 * [normalSetNumber] is 1-based among this exercise's Normal sets, `null` for Warm-up/Drop/Failure
 * (Q3). [weight]/[reps] are `null` when the set is untouched and still at its zero value (blank
 * field); [isPrefilled] marks a value copied in by Q4's prefill rules, shown muted until touched.
 */
data class LiveSetUi(
    val id: WorkoutSetId,
    val type: SetType,
    val normalSetNumber: Int?,
    val weight: Weight?,
    val reps: Int?,
    val previous: PreviousSetUi?,
    val isPrefilled: Boolean,
    val isCompleted: Boolean,
)

data class PreviousSetUi(
    val weight: Weight,
    val reps: Int,
)

data class DiscardConfirmationUi(
    val workoutName: String,
    val completedSetCount: Int,
)

data class LiveSetValuesUi(
    val weight: Weight?,
    val reps: Int?,
    val rpe: Rpe?,
)

/**
 * The number keypad's target for the field in [editingField] (§8). [last] is "Same as last" (T12): the
 * previous set of the same exercise in this workout, or the Previous-column set for the first one;
 * `null` hides the chip. [isLastSetOfWorkout] makes Next hide the keypad on the workout's last field.
 */
data class LiveKeypadTargetUi(
    val setId: WorkoutSetId,
    val field: SetField,
    val exerciseName: String?,
    val normalSetNumber: Int?,
    val setType: SetType,
    val current: LiveSetValuesUi,
    val last: LiveSetValuesUi?,
    val isLastSetOfWorkout: Boolean,
)
