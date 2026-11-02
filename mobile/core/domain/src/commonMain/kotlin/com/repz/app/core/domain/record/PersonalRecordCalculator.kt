package com.repz.app.core.domain.record

import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.SetType
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.model.Workout
import com.repz.app.core.domain.model.WorkoutId
import com.repz.app.core.domain.model.WorkoutSetId
import kotlin.time.Instant

/** One counted set, flattened out of its [Workout]/`WorkoutExercise` with only what a record needs. */
private data class CountedSet(
    val exerciseId: ExerciseId,
    val weight: Weight,
    val reps: Int,
    val workoutId: WorkoutId,
    val setId: WorkoutSetId,
    val completedAt: Instant,
)

/**
 * Recomputes every exercise's personal records from the given workout history: only finished
 * workouts, only completed sets with at least one rep and any type except warm-up. Among equal
 * values, the earliest set (by `completedAt`, then `workoutId`, then `setId`) keeps the record. A
 * record exists only when its value is greater than zero, so a bodyweight exercise can only ever
 * hold [PersonalRecordType.MOST_REPS].
 */
fun calculatePersonalRecords(workouts: List<Workout>): List<PersonalRecord> =
    countedSets(workouts)
        .groupBy { it.exerciseId }
        .flatMap { (exerciseId, sets) -> PersonalRecordType.entries.mapNotNull { type -> bestRecord(exerciseId, type, sets) } }
        .sortedWith(compareBy({ it.exerciseId.value.toString() }, { it.type.ordinal }))

private fun countedSets(workouts: List<Workout>): List<CountedSet> =
    workouts
        .filter { it.isFinished }
        .flatMap { workout ->
            workout.exercises.flatMap { exercise ->
                exercise.sets.mapNotNull { set ->
                    val completedAt = set.completedAt ?: return@mapNotNull null
                    if (set.type == SetType.WARMUP || set.reps < 1) {
                        null
                    } else {
                        CountedSet(exercise.exerciseId, set.weight, set.reps, workout.id, set.id, completedAt)
                    }
                }
            }
        }

private fun bestRecord(
    exerciseId: ExerciseId,
    type: PersonalRecordType,
    sets: List<CountedSet>,
): PersonalRecord? {
    val valueOf = valueSelector(type)
    val contenders = sets.filter { valueOf(it) > 0 }

    val winner =
        contenders.maxOfOrNull(valueOf)?.let { bestValue ->
            contenders
                .filter { valueOf(it) == bestValue }
                .minWithOrNull(compareBy({ it.completedAt }, { it.workoutId.value.toString() }, { it.setId.value.toString() }))
        }

    return winner?.let { set ->
        PersonalRecord(
            exerciseId = exerciseId,
            type = type,
            weight = set.weight,
            reps = set.reps,
            workoutId = set.workoutId,
            setId = set.setId,
            achievedAt = set.completedAt,
        )
    }
}

private fun valueSelector(type: PersonalRecordType): (CountedSet) -> Long =
    when (type) {
        PersonalRecordType.EST_1RM -> { set -> estimateOneRepMax(set.weight, set.reps).grams }
        PersonalRecordType.HEAVIEST -> { set -> set.weight.grams }
        PersonalRecordType.BEST_SET_VOLUME -> { set -> (set.weight * set.reps).grams }
        PersonalRecordType.MOST_REPS -> { set -> set.reps.toLong() }
    }
