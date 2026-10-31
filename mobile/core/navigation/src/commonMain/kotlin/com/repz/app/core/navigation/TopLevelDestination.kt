package com.repz.app.core.navigation

import kotlinx.serialization.Serializable

/**
 * The app's top-level tabs. [Navigator] keeps one back stack per destination; [Train] is the
 * start tab after sign-in.
 */
@Serializable
enum class TopLevelDestination {
    Train,
    History,
    Exercises,
    Profile,
}

val   ciNegativeKtlintBreak=1
