package com.repz.app.core.domain.preferences

import com.repz.app.core.domain.model.UnitSystem
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * The effective preferences the rest of the app reads. Device-wide and surviving logout, except
 * [unitSystem]: the device preference if one was set on this device, else the account's
 * `current_user.unit_system`, else [UnitSystem.KG]; logout clears the device preference.
 */
data class UserPreferences(
    val unitSystem: UnitSystem,
    val themeMode: ThemeMode,
    val dynamicColorEnabled: Boolean,
    val defaultRest: Duration,
    val hapticsEnabled: Boolean,
    val weeklyWorkoutGoal: Int,
) {
    companion object {
        val DEFAULT =
            UserPreferences(
                unitSystem = UnitSystem.KG,
                themeMode = ThemeMode.SYSTEM,
                dynamicColorEnabled = false,
                defaultRest = 150.seconds,
                hapticsEnabled = true,
                weeklyWorkoutGoal = 4,
            )
    }
}
