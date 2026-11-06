package com.repz.app.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import okio.IOException

private val UNIT_SYSTEM_KEY = stringPreferencesKey("unit_system")
private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
private val DYNAMIC_COLOR_ENABLED_KEY = booleanPreferencesKey("dynamic_color_enabled")
private val DEFAULT_REST_SECONDS_KEY = intPreferencesKey("default_rest_seconds")
private val HAPTICS_ENABLED_KEY = booleanPreferencesKey("haptics_enabled")
private val WEEKLY_WORKOUT_GOAL_KEY = intPreferencesKey("weekly_workout_goal")

private const val LOG_TAG = "UserPreferencesDataSource"

/**
 * Raw DataStore access for user preferences: no defaults and no vocabulary mapping (both live in
 * `core:data`, which is the only layer that knows the domain types), the same way `core:database`'s
 * DAOs know only entities. Koin-free: `core:datastore` has no Koin dependency.
 */
class UserPreferencesDataSource(
    private val dataStore: DataStore<Preferences>,
) {
    private val logger = Logger.withTag(LOG_TAG)

    /**
     * A failed read is DataStore's documented [IOException] case; it is logged and treated as an
     * empty file (every field null) rather than crashing the collector (AGENTS.md §4, "no
     * exceptions across layers").
     */
    val preferences: Flow<StoredUserPreferences> =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    logger.e(exception) { "Failed to read preferences; treating as empty." }
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { it.toStoredUserPreferences() }

    /** `null` removes the stored key, so the effective unit falls back to the account's. */
    suspend fun setUnitSystem(value: String?) {
        dataStore.edit { preferences ->
            if (value == null) {
                preferences.remove(UNIT_SYSTEM_KEY)
            } else {
                preferences[UNIT_SYSTEM_KEY] = value
            }
        }
    }

    suspend fun setThemeMode(value: String) {
        dataStore.edit { preferences -> preferences[THEME_MODE_KEY] = value }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[DYNAMIC_COLOR_ENABLED_KEY] = enabled }
    }

    suspend fun setDefaultRestSeconds(seconds: Int) {
        dataStore.edit { preferences -> preferences[DEFAULT_REST_SECONDS_KEY] = seconds }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[HAPTICS_ENABLED_KEY] = enabled }
    }

    suspend fun setWeeklyWorkoutGoal(goal: Int) {
        dataStore.edit { preferences -> preferences[WEEKLY_WORKOUT_GOAL_KEY] = goal }
    }
}

private fun Preferences.toStoredUserPreferences() =
    StoredUserPreferences(
        unitSystem = this[UNIT_SYSTEM_KEY],
        themeMode = this[THEME_MODE_KEY],
        dynamicColorEnabled = this[DYNAMIC_COLOR_ENABLED_KEY],
        defaultRestSeconds = this[DEFAULT_REST_SECONDS_KEY],
        hapticsEnabled = this[HAPTICS_ENABLED_KEY],
        weeklyWorkoutGoal = this[WEEKLY_WORKOUT_GOAL_KEY],
    )
