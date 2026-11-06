package com.repz.app.feature.workout.impl.domain

import com.repz.app.core.domain.exercise.ExerciseRepository
import com.repz.app.core.domain.model.Exercise
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

/** The exercise picker v0's plain list. TODO(REP-37): adds search, filters and multi-select. */
@Factory
class ObservePickerExercisesUseCase(
    private val exercises: ExerciseRepository,
) {
    operator fun invoke(): Flow<List<Exercise>> = exercises.observeExercises()
}
