package com.repz.app.shared.shell.ui

import com.repz.app.core.designsystem.generated.resources.symbol_exercise
import com.repz.app.core.designsystem.generated.resources.symbol_exercise_filled
import com.repz.app.core.designsystem.generated.resources.symbol_fitness_center
import com.repz.app.core.designsystem.generated.resources.symbol_fitness_center_filled
import com.repz.app.core.designsystem.generated.resources.symbol_history
import com.repz.app.core.designsystem.generated.resources.symbol_history_filled
import com.repz.app.core.designsystem.generated.resources.symbol_person
import com.repz.app.core.designsystem.generated.resources.symbol_person_filled
import com.repz.app.core.navigation.TopLevelDestination
import com.repz.app.shared.generated.resources.Res
import com.repz.app.shared.generated.resources.nav_exercises
import com.repz.app.shared.generated.resources.nav_history
import com.repz.app.shared.generated.resources.nav_profile
import com.repz.app.shared.generated.resources.nav_train
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import com.repz.app.core.designsystem.generated.resources.Res as DesignSystemRes

/** The nav bar label for [this] tab (DESIGN_SYSTEM §9, boards 04/08/13). */
internal val TopLevelDestination.label: StringResource
    get() =
        when (this) {
            TopLevelDestination.Train -> Res.string.nav_train
            TopLevelDestination.History -> Res.string.nav_history
            TopLevelDestination.Exercises -> Res.string.nav_exercises
            TopLevelDestination.Profile -> Res.string.nav_profile
        }

/** The nav bar icon for [this] tab while unselected (FILL 0, board 04). */
internal val TopLevelDestination.icon: DrawableResource
    get() =
        when (this) {
            TopLevelDestination.Train -> DesignSystemRes.drawable.symbol_fitness_center
            TopLevelDestination.History -> DesignSystemRes.drawable.symbol_history
            TopLevelDestination.Exercises -> DesignSystemRes.drawable.symbol_exercise
            TopLevelDestination.Profile -> DesignSystemRes.drawable.symbol_person
        }

/** The nav bar icon for [this] tab while selected (FILL 1, board 04). */
internal val TopLevelDestination.selectedIcon: DrawableResource
    get() =
        when (this) {
            TopLevelDestination.Train -> DesignSystemRes.drawable.symbol_fitness_center_filled
            TopLevelDestination.History -> DesignSystemRes.drawable.symbol_history_filled
            TopLevelDestination.Exercises -> DesignSystemRes.drawable.symbol_exercise_filled
            TopLevelDestination.Profile -> DesignSystemRes.drawable.symbol_person_filled
        }
