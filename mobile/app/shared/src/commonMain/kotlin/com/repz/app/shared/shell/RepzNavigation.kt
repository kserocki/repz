package com.repz.app.shared.shell

import androidx.navigation3.runtime.NavKey
import com.repz.app.core.navigation.TopLevelDestination
import com.repz.app.core.navigation.navigationSavedStateConfiguration
import com.repz.app.feature.auth.api.authNavKeySerializers
import com.repz.app.feature.exercises.api.ExercisesRoute
import com.repz.app.feature.exercises.api.exercisesNavKeySerializers
import com.repz.app.feature.history.api.HistoryRoute
import com.repz.app.feature.history.api.historyNavKeySerializers
import com.repz.app.feature.settings.api.ProfileRoute
import com.repz.app.feature.settings.api.settingsNavKeySerializers
import com.repz.app.feature.workout.api.TrainRoute
import com.repz.app.feature.workout.api.workoutNavKeySerializers
import kotlinx.serialization.modules.plus

/** [com.repz.app.core.navigation.rememberNavigator]'s root key for every tab (§3.5). */
internal val RepzTopLevelRoots: Map<TopLevelDestination, NavKey> =
    mapOf(
        TopLevelDestination.Train to TrainRoute,
        TopLevelDestination.History to HistoryRoute,
        TopLevelDestination.Exercises to ExercisesRoute,
        TopLevelDestination.Profile to ProfileRoute,
    )

/** Every `feature:x:api`'s `xNavKeySerializers`, combined. A forgotten one fails [RepzNavKeySerializersTest]. */
internal val RepzNavKeySerializers =
    authNavKeySerializers + workoutNavKeySerializers + historyNavKeySerializers + exercisesNavKeySerializers + settingsNavKeySerializers

internal val RepzNavigationConfiguration = navigationSavedStateConfiguration(RepzNavKeySerializers)
