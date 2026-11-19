package com.repz.app.core.data.preferences

import co.touchlab.kermit.Logger
import com.repz.app.core.database.account.CurrentUserEntity
import com.repz.app.core.datastore.StoredUserPreferences
import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.preferences.ThemeMode
import com.repz.app.core.domain.preferences.UserPreferences
import com.repz.app.core.domain.preferences.WeightStep
import kotlin.time.Duration.Companion.seconds

/**
 * Applies every default (R4) and the unit fallback chain (Q1: device preference, then the
 * account's cached `current_user.unit_system`, then [UnitSystem.KG]). An unknown stored vocabulary
 * text (R5) is logged and treated as not set, falling through the same chain.
 */
internal fun StoredUserPreferences.toDomain(
    currentUser: CurrentUserEntity?,
    logger: Logger,
): UserPreferences {
    val deviceUnit = unitSystem?.toUnitSystemOrNull(logger)
    val accountUnit = currentUser?.unitSystem?.toUnitSystemOrNull(logger)
    return UserPreferences(
        unitSystem = deviceUnit ?: accountUnit ?: UnitSystem.KG,
        themeMode = themeMode?.toThemeModeOrNull(logger) ?: UserPreferences.DEFAULT.themeMode,
        dynamicColorEnabled = dynamicColorEnabled ?: UserPreferences.DEFAULT.dynamicColorEnabled,
        defaultRest = defaultRestSeconds?.seconds ?: UserPreferences.DEFAULT.defaultRest,
        hapticsEnabled = hapticsEnabled ?: UserPreferences.DEFAULT.hapticsEnabled,
        weeklyWorkoutGoal = weeklyWorkoutGoal ?: UserPreferences.DEFAULT.weeklyWorkoutGoal,
        weightStepKg = weightStepKg?.toWeightStepOrNull(UnitSystem.KG, logger) ?: UserPreferences.DEFAULT.weightStepKg,
        weightStepLb = weightStepLb?.toWeightStepOrNull(UnitSystem.LB, logger) ?: UserPreferences.DEFAULT.weightStepLb,
    )
}

private fun String.toUnitSystemOrNull(logger: Logger): UnitSystem? =
    when (this) {
        "kg" -> {
            UnitSystem.KG
        }

        "lb" -> {
            UnitSystem.LB
        }

        else -> {
            logger.e { "Unknown unit_system value: $this" }
            null
        }
    }

/** Matches the stored enum name against [WeightStep]'s values for [unitSystem]; anything else is unknown. */
private fun String.toWeightStepOrNull(
    unitSystem: UnitSystem,
    logger: Logger,
): WeightStep? =
    WeightStep.entries.firstOrNull { it.name == this && it.unitSystem == unitSystem } ?: run {
        logger.e { "Unknown weight_step value for $unitSystem: $this" }
        null
    }

private fun String.toThemeModeOrNull(logger: Logger): ThemeMode? =
    when (this) {
        "system" -> {
            ThemeMode.SYSTEM
        }

        "light" -> {
            ThemeMode.LIGHT
        }

        "dark" -> {
            ThemeMode.DARK
        }

        else -> {
            logger.e { "Unknown theme_mode value: $this" }
            null
        }
    }

internal fun UnitSystem.toWireText(): String =
    when (this) {
        UnitSystem.KG -> "kg"
        UnitSystem.LB -> "lb"
    }

internal fun ThemeMode.toWireText(): String =
    when (this) {
        ThemeMode.SYSTEM -> "system"
        ThemeMode.LIGHT -> "light"
        ThemeMode.DARK -> "dark"
    }

/** Stored as the enum's own name, unlike [UnitSystem]/[ThemeMode]'s hand-written vocabulary text. */
internal fun WeightStep.toWireText(): String = name
