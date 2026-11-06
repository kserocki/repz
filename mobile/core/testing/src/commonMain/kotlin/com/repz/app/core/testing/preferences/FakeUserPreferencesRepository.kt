package com.repz.app.core.testing.preferences

import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.preferences.ThemeMode
import com.repz.app.core.domain.preferences.UserPreferences
import com.repz.app.core.domain.preferences.UserPreferencesRepository
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration

/**
 * A hand-written fake (N7): a single [MutableStateFlow] holds the current preferences.
 * [setDefaultRestResult]/[setWeeklyWorkoutGoalResult] are returned as-is, without writing, when
 * configured to a [DomainResult.Failure]; every other setter always succeeds.
 */
class FakeUserPreferencesRepository(
    initial: UserPreferences = UserPreferences.DEFAULT,
    var setDefaultRestResult: DomainResult<Unit, ValidationError.RestOutOfRange>? = null,
    var setWeeklyWorkoutGoalResult: DomainResult<Unit, ValidationError.WeeklyWorkoutGoalOutOfRange>? = null,
) : UserPreferencesRepository {
    private val preferences = MutableStateFlow(initial)

    override fun observePreferences(): Flow<UserPreferences> = preferences

    override suspend fun setUnitSystem(unitSystem: UnitSystem) {
        preferences.update { it.copy(unitSystem = unitSystem) }
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        preferences.update { it.copy(themeMode = themeMode) }
    }

    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        preferences.update { it.copy(dynamicColorEnabled = enabled) }
    }

    override suspend fun setDefaultRest(rest: Duration): DomainResult<Unit, ValidationError.RestOutOfRange> {
        setDefaultRestResult?.let { return it }
        preferences.update { it.copy(defaultRest = rest) }
        return DomainResult.Success(Unit)
    }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        preferences.update { it.copy(hapticsEnabled = enabled) }
    }

    override suspend fun setWeeklyWorkoutGoal(goal: Int): DomainResult<Unit, ValidationError.WeeklyWorkoutGoalOutOfRange> {
        setWeeklyWorkoutGoalResult?.let { return it }
        preferences.update { it.copy(weeklyWorkoutGoal = goal) }
        return DomainResult.Success(Unit)
    }
}
