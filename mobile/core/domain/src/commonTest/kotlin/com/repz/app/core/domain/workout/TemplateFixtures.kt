package com.repz.app.core.domain.workout

import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.TemplateExercise
import com.repz.app.core.domain.model.WorkoutTemplate
import com.repz.app.core.domain.model.WorkoutTemplateId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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
    notes: String = "",
    exercises: List<TemplateExercise> = listOf(testTemplateExercise(seed)),
): WorkoutTemplate = WorkoutTemplate(id = WorkoutTemplateId(testUuid(seed)), name = name, notes = notes, exercises = exercises)
