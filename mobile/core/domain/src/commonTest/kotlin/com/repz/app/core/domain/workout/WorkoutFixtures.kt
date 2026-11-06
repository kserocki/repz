package com.repz.app.core.domain.workout

import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.SetType
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.model.Workout
import com.repz.app.core.domain.model.WorkoutExercise
import com.repz.app.core.domain.model.WorkoutExerciseId
import com.repz.app.core.domain.model.WorkoutId
import com.repz.app.core.domain.model.WorkoutSet
import com.repz.app.core.domain.model.WorkoutSetId
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * A deterministic, literal id for tests (Konsist I1 forbids `Uuid.random` in tests too): [seed]
 * zero-padded into an otherwise-fixed UUID. Shared across the use case/validation test files in
 * this module.
 */
internal fun testUuid(seed: Int): Uuid = Uuid.parse("00000000-0000-0000-0000-" + seed.toString().padStart(12, '0'))

internal fun testWorkoutSet(
    seed: Int,
    weight: Weight = Weight.fromKilograms(50.0),
    reps: Int = 5,
    completedAt: Instant? = Instant.fromEpochSeconds(seed.toLong()),
): WorkoutSet =
    WorkoutSet(
        id = WorkoutSetId(testUuid(seed)),
        type = SetType.NORMAL,
        weight = weight,
        reps = reps,
        rpe = null,
        completedAt = completedAt,
    )

internal fun testWorkoutExercise(
    seed: Int,
    exerciseId: ExerciseId = ExerciseId(testUuid(seed)),
    notes: String = "",
    sets: List<WorkoutSet> = listOf(testWorkoutSet(seed)),
): WorkoutExercise =
    WorkoutExercise(
        id = WorkoutExerciseId(testUuid(seed)),
        exerciseId = exerciseId,
        notes = notes,
        sets = sets,
    )

internal fun testFinishedWorkout(
    seed: Int = 1,
    name: String = "Push day",
    notes: String = "",
    startedAt: Instant = Instant.fromEpochSeconds(0),
    finishedAt: Instant? = Instant.fromEpochSeconds(3600),
    exercises: List<WorkoutExercise> = listOf(testWorkoutExercise(seed)),
): Workout =
    Workout(
        id = WorkoutId(testUuid(seed)),
        templateId = null,
        name = name,
        notes = notes,
        startedAt = startedAt,
        finishedAt = finishedAt,
        exercises = exercises,
    )

/** [testFinishedWorkout]'s active-workout counterpart: `finishedAt = null`. */
internal fun testActiveWorkout(
    seed: Int = 1,
    name: String = "Push day",
    notes: String = "",
    startedAt: Instant = Instant.fromEpochSeconds(0),
    exercises: List<WorkoutExercise> = listOf(testWorkoutExercise(seed)),
): Workout =
    Workout(
        id = WorkoutId(testUuid(seed)),
        templateId = null,
        name = name,
        notes = notes,
        startedAt = startedAt,
        finishedAt = null,
        exercises = exercises,
    )
