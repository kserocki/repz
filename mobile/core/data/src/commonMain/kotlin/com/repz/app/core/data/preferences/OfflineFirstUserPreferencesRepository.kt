package com.repz.app.core.data.preferences

import co.touchlab.kermit.Logger
import com.repz.app.core.database.account.CurrentUserDao
import com.repz.app.core.datastore.UserPreferencesDataSource
import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.preferences.ThemeMode
import com.repz.app.core.domain.preferences.UserPreferences
import com.repz.app.core.domain.preferences.UserPreferencesRepository
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import com.repz.app.core.domain.validation.ValidationLimits
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.koin.core.annotation.Single
import kotlin.time.Duration

internal const val PREFERENCES_LOG_TAG = "UserPreferences"

/**
 * Device-wide app preferences (rule 6), over `core:datastore`'s raw [UserPreferencesDataSource] and
 * the cached [CurrentUserDao] row the unit fallback chain reads (Q1). [setUnitSystem] only ever
 * writes the device preference; logout clears it separately, from
 * [com.repz.app.core.data.auth.OfflineFirstAuthRepository.logOut].
 */
@Single
class OfflineFirstUserPreferencesRepository(
    private val dataSource: UserPreferencesDataSource,
    private val currentUserDao: CurrentUserDao,
    private val logger: Logger = Logger.withTag(PREFERENCES_LOG_TAG),
) : UserPreferencesRepository {
    override fun observePreferences(): Flow<UserPreferences> =
        combine(dataSource.preferences, currentUserDao.observe()) { stored, currentUser -> stored.toDomain(currentUser, logger) }

    override suspend fun setUnitSystem(unitSystem: UnitSystem) {
        dataSource.setUnitSystem(unitSystem.toWireText())
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataSource.setThemeMode(themeMode.toWireText())
    }

    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataSource.setDynamicColorEnabled(enabled)
    }

    override suspend fun setDefaultRest(rest: Duration): DomainResult<Unit, ValidationError.RestOutOfRange> =
        when (val validation = ValidationLimits.validateRest(rest)) {
            is DomainResult.Success -> {
                dataSource.setDefaultRestSeconds(validation.value.inWholeSeconds.toInt())
                DomainResult.Success(Unit)
            }

            is DomainResult.Failure -> {
                validation
            }
        }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        dataSource.setHapticsEnabled(enabled)
    }

    override suspend fun setWeeklyWorkoutGoal(goal: Int): DomainResult<Unit, ValidationError.WeeklyWorkoutGoalOutOfRange> =
        when (val validation = ValidationLimits.validateWeeklyWorkoutGoal(goal)) {
            is DomainResult.Success -> {
                dataSource.setWeeklyWorkoutGoal(validation.value)
                DomainResult.Success(Unit)
            }

            is DomainResult.Failure -> {
                validation
            }
        }
}
