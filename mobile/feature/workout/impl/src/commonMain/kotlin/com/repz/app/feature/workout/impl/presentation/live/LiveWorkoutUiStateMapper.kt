package com.repz.app.feature.workout.impl.presentation.live

import com.repz.app.core.domain.model.SetType
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.model.Workout
import com.repz.app.core.domain.model.WorkoutExercise
import com.repz.app.core.domain.model.WorkoutSet
import com.repz.app.core.domain.model.localizedName
import com.repz.app.core.domain.validation.ValidationLimits
import com.repz.app.core.domain.workout.completedSetCount
import com.repz.app.core.domain.workout.findSet
import com.repz.app.feature.workout.impl.domain.LiveWorkoutSnapshot
import kotlinx.collections.immutable.toImmutableList
import kotlin.time.Clock
import kotlin.time.Duration

/** Maps the reactive [LiveWorkoutSnapshot] and the in-memory [LocalState] to [LiveWorkoutUiState]. */
internal class LiveWorkoutUiStateMapper(
    private val languageTag: () -> String,
    private val clock: Clock,
) {
    fun toUiState(
        snapshot: LiveWorkoutSnapshot?,
        local: LocalState,
    ): LiveWorkoutUiState {
        if (snapshot == null) {
            return LiveWorkoutUiState(isLoading = true, isFinishing = local.isFinishing)
        }
        val workout = snapshot.workout
        return LiveWorkoutUiState(
            isLoading = false,
            workoutName = local.nameDraft ?: workout.name,
            isEditingName = local.nameDraft != null,
            elapsed = (clock.now() - workout.startedAt).coerceAtLeast(Duration.ZERO),
            unit = snapshot.unit,
            exercises = workout.exercises.map { exercise -> toExerciseUi(exercise, snapshot, local) }.toImmutableList(),
            canFinish = workout.completedSetCount > 0,
            isFinishing = local.isFinishing,
            canAddExercise = workout.exercises.size < ValidationLimits.EXERCISES_PER_WORKOUT_MAX,
            isReordering = local.isReordering,
            editingField = local.editingField,
            keypadTarget = local.editingField?.let { toKeypadTargetUi(it, snapshot) },
            weightStep = snapshot.weightStep,
            discardConfirmation = local.discardConfirmation,
        )
    }

    private fun toExerciseUi(
        exercise: WorkoutExercise,
        snapshot: LiveWorkoutSnapshot,
        local: LocalState,
    ): LiveExerciseUi {
        val domainExercise = snapshot.exercisesById[exercise.exerciseId]
        val lastPerformanceSets = snapshot.lastPerformanceByExercise[exercise.exerciseId]
        val isEditingNotes = local.editingNotesExerciseId == exercise.id
        return LiveExerciseUi(
            id = exercise.id,
            name = domainExercise?.localizedName(languageTag()),
            primaryMuscle = domainExercise?.primaryMuscle,
            notes = if (isEditingNotes) local.notesDraft else exercise.notes,
            isEditingNotes = isEditingNotes,
            completedSetCount = exercise.sets.count { it.isCompleted },
            canAddSet = exercise.sets.size < ValidationLimits.SETS_PER_EXERCISE_MAX,
            sets =
                exercise.sets
                    .mapIndexed { index, set -> toSetUi(set, index, exercise.sets, lastPerformanceSets, local) }
                    .toImmutableList(),
        )
    }

    private fun toSetUi(
        set: WorkoutSet,
        position: Int,
        setsInExercise: List<WorkoutSet>,
        lastPerformanceSets: List<WorkoutSet>?,
        local: LocalState,
    ): LiveSetUi {
        val isTouched = set.id in local.touchedSetIds
        val previousSet = lastPerformanceSets?.getOrNull(position)
        return LiveSetUi(
            id = set.id,
            type = set.type,
            normalSetNumber = normalSetNumberAt(setsInExercise, position),
            weight = displayWeight(set, isTouched),
            reps = displayReps(set, isTouched),
            previous = previousSet?.let { PreviousSetUi(it.weight, it.reps) },
            isPrefilled = !isTouched && !set.isCompleted && (set.weight != Weight.ZERO || set.reps != 0),
            isCompleted = set.isCompleted,
        )
    }

    private fun toKeypadTargetUi(
        fieldRef: SetFieldRef,
        snapshot: LiveWorkoutSnapshot,
    ): LiveKeypadTargetUi? {
        val (exercise, index) = snapshot.workout.findSet(fieldRef.setId) ?: return null
        val set = exercise.sets[index]
        val lastPerformanceSets = snapshot.lastPerformanceByExercise[exercise.exerciseId]
        return LiveKeypadTargetUi(
            setId = set.id,
            field = fieldRef.field,
            exerciseName = snapshot.exercisesById[exercise.exerciseId]?.localizedName(languageTag()),
            normalSetNumber = normalSetNumberAt(exercise.sets, index),
            setType = set.type,
            current = LiveSetValuesUi(weight = set.weight, reps = set.reps, rpe = set.rpe),
            last = sameAsLast(exercise.sets, index, lastPerformanceSets),
            isLastSetOfWorkout = isLastSetOfWorkout(snapshot.workout, exercise, index),
        )
    }
}

/** 1-based among this exercise's Normal sets at [position]; `null` for a Warm-up/Drop/Failure set. */
private fun normalSetNumberAt(
    sets: List<WorkoutSet>,
    position: Int,
): Int? = if (sets[position].type != SetType.NORMAL) null else sets.take(position + 1).count { it.type == SetType.NORMAL }

/** "Same as last": the previous set of the same exercise in this workout, else the Previous-column set, else `null`. */
private fun sameAsLast(
    setsInExercise: List<WorkoutSet>,
    position: Int,
    lastPerformanceSets: List<WorkoutSet>?,
): LiveSetValuesUi? {
    val previousSetInThisWorkout = setsInExercise.getOrNull(position - 1)
    val source = previousSetInThisWorkout ?: lastPerformanceSets?.getOrNull(0)
    return source?.let { LiveSetValuesUi(it.weight, it.reps, it.rpe) }
}

private fun isLastSetOfWorkout(
    workout: Workout,
    exercise: WorkoutExercise,
    index: Int,
): Boolean = index == exercise.sets.lastIndex && workout.exercises.lastOrNull()?.id == exercise.id

/** `null` = blank (untouched and 0). */
private fun displayWeight(
    set: WorkoutSet,
    isTouched: Boolean,
): Weight? = if (!isTouched && set.weight == Weight.ZERO) null else set.weight

/** `null` = blank (untouched and 0). */
private fun displayReps(
    set: WorkoutSet,
    isTouched: Boolean,
): Int? = if (!isTouched && set.reps == 0) null else set.reps
