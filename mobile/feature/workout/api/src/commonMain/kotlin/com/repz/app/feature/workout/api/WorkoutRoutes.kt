package com.repz.app.feature.workout.api

import androidx.navigation3.runtime.NavKey
import com.repz.app.core.navigation.navKeySerializers
import kotlinx.serialization.Serializable

/** The Train tab's root. */
@Serializable
data object TrainRoute : NavKey

/** The one active workout: no id, since at most one can exist at a time (ADR-0023/ADR-0029). */
@Serializable
data object LiveWorkoutRoute : NavKey

/** Adds exercises to the active workout, or replaces [replacingWorkoutExerciseId]'s exercise when set (a `WorkoutExerciseId`). */
@Serializable
data class ExercisePickerRoute(
    val replacingWorkoutExerciseId: String? = null,
) : NavKey

val workoutNavKeySerializers =
    navKeySerializers {
        subclass(TrainRoute::class, TrainRoute.serializer())
        subclass(LiveWorkoutRoute::class, LiveWorkoutRoute.serializer())
        subclass(ExercisePickerRoute::class, ExercisePickerRoute.serializer())
    }
