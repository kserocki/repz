package com.repz.app.core.domain.preferences

import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

/**
 * Device-wide app preferences (rule 6: the UI reads only from Room/DataStore through here), except
 * [UserPreferences.unitSystem]: [setUnitSystem] stores it per device, and logout clears it, because
 * it belongs to the account.
 */
interface UserPreferencesRepository {
    fun observePreferences(): Flow<UserPreferences>

    suspend fun setUnitSystem(unitSystem: UnitSystem)

    suspend fun setThemeMode(themeMode: ThemeMode)

    suspend fun setDynamicColorEnabled(enabled: Boolean)

    suspend fun setDefaultRest(rest: Duration): DomainResult<Unit, ValidationError.RestOutOfRange>

    suspend fun setHapticsEnabled(enabled: Boolean)

    suspend fun setWeeklyWorkoutGoal(goal: Int): DomainResult<Unit, ValidationError.WeeklyWorkoutGoalOutOfRange>
}
