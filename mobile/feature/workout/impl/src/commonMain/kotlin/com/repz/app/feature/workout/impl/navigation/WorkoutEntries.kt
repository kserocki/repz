package com.repz.app.feature.workout.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.repz.app.core.navigation.Navigator
import com.repz.app.feature.workout.api.ExercisePickerRoute
import com.repz.app.feature.workout.api.LiveWorkoutRoute
import com.repz.app.feature.workout.api.TrainRoute
import com.repz.app.feature.workout.impl.presentation.live.LiveWorkoutScreen
import com.repz.app.feature.workout.impl.presentation.picker.ExercisePickerScreen
import com.repz.app.feature.workout.impl.presentation.train.TrainScreen

/** The Train tab's root, the live workout and the exercise picker. Each screen resolves its own
 * ViewModel and navigates through [navigator] itself (ADR-0005). */
fun EntryProviderScope<NavKey>.workoutEntries(navigator: Navigator) {
    entry<TrainRoute> { TrainScreen(navigator) }
    entry<LiveWorkoutRoute> { LiveWorkoutScreen(navigator) }
    entry<ExercisePickerRoute> { ExercisePickerScreen(navigator) }
}
