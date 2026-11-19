package com.repz.app.core.datastore

/**
 * Raw values as stored on disk, before `core:data` applies defaults or maps vocabulary text to
 * domain enums. `null` means the key was never set.
 */
data class StoredUserPreferences(
    val unitSystem: String?,
    val themeMode: String?,
    val dynamicColorEnabled: Boolean?,
    val defaultRestSeconds: Int?,
    val hapticsEnabled: Boolean?,
    val weeklyWorkoutGoal: Int?,
    val weightStepKg: String?,
    val weightStepLb: String?,
)
