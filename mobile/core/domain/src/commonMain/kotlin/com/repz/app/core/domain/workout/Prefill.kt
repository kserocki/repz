package com.repz.app.core.domain.workout

import com.repz.app.core.domain.model.SetType
import com.repz.app.core.domain.model.TemplateExercise
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.model.WorkoutExercise
import com.repz.app.core.domain.model.WorkoutSet
import com.repz.app.core.domain.model.WorkoutSetId

// Pure prefill rules (ADR-0029), shared by the start use cases and the feature's add-exercise/
// add-set flows. Every returned set is uncompleted (completedAt = null) with no RPE; newSetId
// mints each new set's id (callers pass their own IdGenerator through it).

/** Adding an exercise from the picker: copies [lastPerformance] position by position; no history gives one empty Normal set. */
fun prefilledSetsForAddedExercise(
    lastPerformance: List<WorkoutSet>?,
    newSetId: () -> WorkoutSetId,
): List<WorkoutSet> {
    val copiedSets = lastPerformance?.map { set -> set.asPrefilledCopy(newSetId()) }.orEmpty()
    return copiedSets.ifEmpty { listOf(emptyNormalSet(newSetId())) }
}

/**
 * Starting from a template: one Normal set per [TemplateExercise.targetSets]; set `i` takes
 * [lastPerformance]'s set `i` when it exists, else a blank weight and reps set to the bottom of
 * [TemplateExercise.targetReps] (or blank with no range).
 */
fun prefilledSetsForTemplateExercise(
    target: TemplateExercise,
    lastPerformance: List<WorkoutSet>?,
    newSetId: () -> WorkoutSetId,
): List<WorkoutSet> =
    (0 until target.targetSets).map { position ->
        val lastSet = lastPerformance?.getOrNull(position)
        if (lastSet != null) {
            lastSet.asPrefilledCopy(newSetId())
        } else {
            WorkoutSet(
                id = newSetId(),
                type = SetType.NORMAL,
                weight = Weight.ZERO,
                reps = target.targetReps?.first ?: 0,
                rpe = null,
                completedAt = null,
            )
        }
    }

/**
 * Add set: a new Normal set copying [lastPerformance]'s set at [exercise]'s next position when it
 * exists, else [exercise]'s current last set, else empty.
 */
fun prefilledSetToAppend(
    exercise: WorkoutExercise,
    lastPerformance: List<WorkoutSet>?,
    newSetId: WorkoutSetId,
): WorkoutSet {
    val lastSetAtPosition = lastPerformance?.getOrNull(exercise.sets.size)
    val exercisesOwnLastSet = exercise.sets.lastOrNull()
    return WorkoutSet(
        id = newSetId,
        type = SetType.NORMAL,
        weight = lastSetAtPosition?.weight ?: exercisesOwnLastSet?.weight ?: Weight.ZERO,
        reps = lastSetAtPosition?.reps ?: exercisesOwnLastSet?.reps ?: 0,
        rpe = null,
        completedAt = null,
    )
}

private fun WorkoutSet.asPrefilledCopy(id: WorkoutSetId): WorkoutSet = copy(id = id, completedAt = null, rpe = null)

private fun emptyNormalSet(id: WorkoutSetId): WorkoutSet =
    WorkoutSet(id = id, type = SetType.NORMAL, weight = Weight.ZERO, reps = 0, rpe = null, completedAt = null)
