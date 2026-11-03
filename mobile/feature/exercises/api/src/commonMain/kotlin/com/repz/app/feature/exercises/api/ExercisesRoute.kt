package com.repz.app.feature.exercises.api

import androidx.navigation3.runtime.NavKey
import com.repz.app.core.navigation.navKeySerializers
import kotlinx.serialization.Serializable

/** The Exercises tab's root. */
@Serializable
data object ExercisesRoute : NavKey

val exercisesNavKeySerializers =
    navKeySerializers {
        subclass(ExercisesRoute::class, ExercisesRoute.serializer())
    }
