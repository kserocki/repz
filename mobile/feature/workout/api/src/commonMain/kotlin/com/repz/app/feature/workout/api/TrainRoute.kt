package com.repz.app.feature.workout.api

import androidx.navigation3.runtime.NavKey
import com.repz.app.core.navigation.navKeySerializers
import kotlinx.serialization.Serializable

/** The Train tab's root. */
@Serializable
data object TrainRoute : NavKey

val workoutNavKeySerializers =
    navKeySerializers {
        subclass(TrainRoute::class, TrainRoute.serializer())
    }
