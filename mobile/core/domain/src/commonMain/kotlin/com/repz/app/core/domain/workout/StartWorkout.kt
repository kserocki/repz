package com.repz.app.core.domain.workout

import com.repz.app.core.common.id.IdGenerator
import com.repz.app.core.domain.model.Workout
import com.repz.app.core.domain.model.WorkoutExercise
import com.repz.app.core.domain.model.WorkoutExerciseId
import com.repz.app.core.domain.model.WorkoutId
import com.repz.app.core.domain.model.WorkoutSetId
import com.repz.app.core.domain.model.WorkoutTemplate
import com.repz.app.core.domain.model.WorkoutTemplateId
import com.repz.app.core.domain.result.DomainError
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.template.WorkoutTemplateRepository
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

sealed interface StartWorkoutError : DomainError {
    data class WorkoutAlreadyActive(
        val activeWorkoutId: WorkoutId,
    ) : StartWorkoutError

    data object TemplateNotFound : StartWorkoutError
}

/** Starts an empty active workout named [name] (T13: already the localized weekday name); fails if one is already active (T7). */
class StartEmptyWorkoutUseCase(
    private val workouts: WorkoutRepository,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
) {
    suspend operator fun invoke(name: String): DomainResult<WorkoutId, StartWorkoutError> {
        val activeWorkout = workouts.observeActiveWorkout().first()
        if (activeWorkout != null) return DomainResult.Failure(StartWorkoutError.WorkoutAlreadyActive(activeWorkout.id))

        val workout =
            Workout(
                id = WorkoutId(idGenerator.newId()),
                templateId = null,
                name = name,
                notes = "",
                startedAt = clock.now(),
                finishedAt = null,
                exercises = emptyList(),
            )
        workouts.saveActiveWorkout(workout)
        return DomainResult.Success(workout.id)
    }
}

/**
 * Starts an active workout from [templateId]: name, exercises and sets per the template
 * (ADR-0029); fails if one is already active (T7) or the template is unknown.
 */
class StartWorkoutFromTemplateUseCase(
    private val workouts: WorkoutRepository,
    private val templates: WorkoutTemplateRepository,
    private val lastPerformance: FindLastPerformanceUseCase,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
) {
    suspend operator fun invoke(templateId: WorkoutTemplateId): DomainResult<WorkoutId, StartWorkoutError> {
        val activeWorkout = workouts.observeActiveWorkout().first()
        if (activeWorkout != null) return DomainResult.Failure(StartWorkoutError.WorkoutAlreadyActive(activeWorkout.id))

        val template = templates.observeTemplate(templateId).first()
        return if (template == null) {
            DomainResult.Failure(StartWorkoutError.TemplateNotFound)
        } else {
            startWorkout(template, templateId)
        }
    }

    private suspend fun startWorkout(
        template: WorkoutTemplate,
        templateId: WorkoutTemplateId,
    ): DomainResult<WorkoutId, StartWorkoutError> {
        val lastPerformanceByExercise = lastPerformance(template.exercises.map { it.exerciseId }.toSet())

        val exercises =
            template.exercises.map { templateExercise ->
                WorkoutExercise(
                    id = WorkoutExerciseId(idGenerator.newId()),
                    exerciseId = templateExercise.exerciseId,
                    notes = "",
                    sets =
                        prefilledSetsForTemplateExercise(
                            target = templateExercise,
                            lastPerformance = lastPerformanceByExercise[templateExercise.exerciseId],
                            newSetId = { WorkoutSetId(idGenerator.newId()) },
                        ),
                )
            }

        val workout =
            Workout(
                id = WorkoutId(idGenerator.newId()),
                templateId = templateId,
                name = template.name,
                notes = "",
                startedAt = clock.now(),
                finishedAt = null,
                exercises = exercises,
            )
        workouts.saveActiveWorkout(workout)
        return DomainResult.Success(workout.id)
    }
}
