package com.repz.app.shared.shell

import androidx.navigation3.runtime.NavKey
import com.repz.app.feature.workout.api.ExercisePickerRoute
import com.repz.app.feature.workout.api.LiveWorkoutRoute

/** Full-screen destinations with no nav bar: the active workout and the exercise picker (T1). */
internal val FullScreenNavKeys: Set<NavKey> = setOf(LiveWorkoutRoute, ExercisePickerRoute)
