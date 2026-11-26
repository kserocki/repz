package com.repz.app.shared.shell

import androidx.navigation3.runtime.NavKey
import com.repz.app.feature.workout.api.ExercisePickerRoute
import com.repz.app.feature.workout.api.LiveWorkoutRoute

/** Full-screen destinations with no nav bar: the active workout and the exercise picker (any mode). */
internal fun NavKey?.isFullScreen(): Boolean = this == LiveWorkoutRoute || this is ExercisePickerRoute
