package com.repz.app.shared.shell

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.repz.app.core.navigation.Navigator
import com.repz.app.core.navigation.TopLevelDestination
import com.repz.app.feature.auth.impl.navigation.authEntries
import com.repz.app.feature.exercises.api.ExercisesRoute
import com.repz.app.feature.history.api.HistoryRoute
import com.repz.app.feature.settings.api.ProfileRoute
import com.repz.app.feature.workout.api.TrainRoute
import com.repz.app.shared.shell.ui.TabRootPlaceholder

/**
 * The one list every feature edits as its screens land: a placeholder for every root key until
 * its real `xEntries(navigator)` function replaces the line (§3.6).
 */
internal fun EntryProviderScope<NavKey>.repzEntries(navigator: Navigator) {
    authEntries(navigator)
    // TODO(REP-32): replace with the real Train home
    entry<TrainRoute> { TabRootPlaceholder(TopLevelDestination.Train) }
    // TODO(REP-40): replace with the real History root
    entry<HistoryRoute> { TabRootPlaceholder(TopLevelDestination.History) }
    // TODO(REP-42): replace with the real Exercises root
    entry<ExercisesRoute> { TabRootPlaceholder(TopLevelDestination.Exercises) }
    // TODO(REP-45): replace with the real Profile root
    entry<ProfileRoute> { TabRootPlaceholder(TopLevelDestination.Profile) }
}
