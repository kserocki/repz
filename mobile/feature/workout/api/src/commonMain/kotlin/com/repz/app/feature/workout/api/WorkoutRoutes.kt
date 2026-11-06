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

/** Adds an exercise to the active workout. TODO(REP-37): grows into the full exercise picker (C13). */
@Serializable
data object ExercisePickerRoute : NavKey

val workoutNavKeySerializers =
    navKeySerializers {
        subclass(TrainRoute::class, TrainRoute.serializer())
        subclass(LiveWorkoutRoute::class, LiveWorkoutRoute.serializer())
        subclass(ExercisePickerRoute::class, ExercisePickerRoute.serializer())
    }
