package com.repz.app.feature.workout.impl

import com.repz.app.core.domain.model.Equipment
import com.repz.app.core.domain.model.Exercise
import com.repz.app.core.domain.model.ExerciseAttribution
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.ExerciseOrigin
import com.repz.app.core.domain.model.ExerciseTranslation
import com.repz.app.core.domain.model.Muscle
import com.repz.app.core.domain.model.SetType
import com.repz.app.core.domain.model.TemplateExercise
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.model.Workout
import com.repz.app.core.domain.model.WorkoutExercise
import com.repz.app.core.domain.model.WorkoutExerciseId
import com.repz.app.core.domain.model.WorkoutId
import com.repz.app.core.domain.model.WorkoutSet
import com.repz.app.core.domain.model.WorkoutSetId
import com.repz.app.core.domain.model.WorkoutTemplate
import com.repz.app.core.domain.model.WorkoutTemplateId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * A deterministic, literal id for tests (Konsist I1 forbids `Uuid.random` in tests too): [seed]
 * zero-padded into an otherwise-fixed UUID.
 */
internal fun testUuid(seed: Int): Uuid = Uuid.parse("00000000-0000-0000-0000-" + seed.toString().padStart(12, '0'))

internal fun testWorkoutSet(
    seed: Int,
    weight: Weight = Weight.fromKilograms(50.0),
    reps: Int = 5,
    type: SetType = SetType.NORMAL,
    completedAt: Instant? = null,
): WorkoutSet =
    WorkoutSet(id = WorkoutSetId(testUuid(seed)), type = type, weight = weight, reps = reps, rpe = null, completedAt = completedAt)

internal fun testWorkoutExercise(
    seed: Int,
    exerciseId: ExerciseId = ExerciseId(testUuid(seed)),
    notes: String = "",
    sets: List<WorkoutSet> = listOf(testWorkoutSet(seed)),
): WorkoutExercise = WorkoutExercise(id = WorkoutExerciseId(testUuid(seed)), exerciseId = exerciseId, notes = notes, sets = sets)

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

internal fun testFinishedWorkout(
    seed: Int = 1,
    name: String = "Push day",
    startedAt: Instant = Instant.fromEpochSeconds(0),
    exercises: List<WorkoutExercise> = listOf(testWorkoutExercise(seed)),
): Workout =
    Workout(
        id = WorkoutId(testUuid(seed)),
        templateId = null,
        name = name,
        notes = "",
        startedAt = startedAt,
        finishedAt = startedAt + 3_600.seconds,
        exercises = exercises,
    )

internal fun testExercise(
    seed: Int,
    name: String = "Exercise $seed",
    primaryMuscle: Muscle = Muscle.CHEST,
    translations: List<ExerciseTranslation> = emptyList(),
): Exercise =
    Exercise(
        id = ExerciseId(testUuid(seed)),
        name = name,
        primaryMuscle = primaryMuscle,
        secondaryMuscles = emptySet(),
        equipment = Equipment.BARBELL,
        origin = ExerciseOrigin.Catalog(imageUrl = null, attribution = ExerciseAttribution("CC0", "Repz", "https://example.com")),
        translations = translations,
    )

internal fun testTemplateExercise(
    seed: Int = 1,
    exerciseId: ExerciseId = ExerciseId(testUuid(seed)),
    targetSets: Int = 3,
    targetReps: IntRange? = 6..8,
    rest: Duration = 90.seconds,
): TemplateExercise = TemplateExercise(exerciseId = exerciseId, targetSets = targetSets, targetReps = targetReps, rest = rest)

internal fun testWorkoutTemplate(
    seed: Int = 1,
    name: String = "Push A",
    exercises: List<TemplateExercise> = listOf(testTemplateExercise(seed)),
): WorkoutTemplate = WorkoutTemplate(id = WorkoutTemplateId(testUuid(seed)), name = name, notes = "", exercises = exercises)
