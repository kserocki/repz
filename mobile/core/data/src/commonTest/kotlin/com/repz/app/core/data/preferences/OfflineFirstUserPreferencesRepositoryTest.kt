package com.repz.app.core.data.preferences

import androidx.room3.Room
import com.repz.app.core.data.newTestUserPreferencesDataSource
import com.repz.app.core.data.testUuid
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.account.CurrentUserEntity
import com.repz.app.core.database.buildRepzDatabase
import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.preferences.ThemeMode
import com.repz.app.core.domain.preferences.UserPreferences
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class OfflineFirstUserPreferencesRepositoryTest {
    // Room needs a real dispatcher for its query/invalidation work, not the runTest scheduler.
    private val database = Room.inMemoryDatabaseBuilder<RepzDatabase>().buildRepzDatabase(Dispatchers.Default)
    private val currentUserDao = database.currentUserDao()
    private val dataSource = newTestUserPreferencesDataSource()
    private val repository = OfflineFirstUserPreferencesRepository(dataSource, currentUserDao)

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun givenAFreshInstall_whenPreferencesObserved_thenEveryFieldIsTheDocumentedDefault() =
        runTest {
            assertEquals(UserPreferences.DEFAULT, repository.observePreferences().first())
        }

    @Test
    fun givenNoDevicePreferenceButACachedAccount_whenUnitRead_thenTheAccountsUnitIsUsed() =
        runTest {
            currentUserDao.replace(userEntity(unitSystem = "lb"))

            assertEquals(UnitSystem.LB, repository.observePreferences().first().unitSystem)
        }

    @Test
    fun givenADevicePreferenceAndADifferentAccountUnit_whenUnitRead_thenTheDevicePreferenceWins() =
        runTest {
            currentUserDao.replace(userEntity(unitSystem = "kg"))
            repository.setUnitSystem(UnitSystem.LB)

            assertEquals(UnitSystem.LB, repository.observePreferences().first().unitSystem)
        }

    @Test
    fun givenNeitherADevicePreferenceNorACachedAccount_whenUnitRead_thenFallsBackToKg() =
        runTest {
            assertEquals(UnitSystem.KG, repository.observePreferences().first().unitSystem)
        }

    @Test
    fun givenEverySetterCalled_whenPreferencesRead_thenEveryFieldRoundTrips() =
        runTest {
            repository.setThemeMode(ThemeMode.DARK)
            repository.setDynamicColorEnabled(true)
            val restResult = repository.setDefaultRest(90.seconds)
            repository.setHapticsEnabled(false)
            val goalResult = repository.setWeeklyWorkoutGoal(5)

            assertEquals(DomainResult.Success(Unit), restResult)
            assertEquals(DomainResult.Success(Unit), goalResult)
            val preferences = repository.observePreferences().first()
            assertEquals(ThemeMode.DARK, preferences.themeMode)
            assertEquals(true, preferences.dynamicColorEnabled)
            assertEquals(90.seconds, preferences.defaultRest)
            assertEquals(false, preferences.hapticsEnabled)
            assertEquals(5, preferences.weeklyWorkoutGoal)
        }

    @Test
    fun givenARestOneSecondOverTheMax_whenSet_thenFailsAndLeavesTheStoredValueUnchanged() =
        runTest {
            val result = repository.setDefaultRest(3601.seconds)

            assertEquals(DomainResult.Failure(ValidationError.RestOutOfRange(3600.seconds)), result)
            assertEquals(UserPreferences.DEFAULT.defaultRest, repository.observePreferences().first().defaultRest)
        }

    @Test
    fun givenAWeeklyGoalOfZeroOrEight_whenSet_thenBothFailAndLeaveTheStoredValueUnchanged() =
        runTest {
            val belowRange = repository.setWeeklyWorkoutGoal(0)
            val aboveRange = repository.setWeeklyWorkoutGoal(8)

            assertEquals(DomainResult.Failure(ValidationError.WeeklyWorkoutGoalOutOfRange(1..7)), belowRange)
            assertEquals(DomainResult.Failure(ValidationError.WeeklyWorkoutGoalOutOfRange(1..7)), aboveRange)
            assertEquals(UserPreferences.DEFAULT.weeklyWorkoutGoal, repository.observePreferences().first().weeklyWorkoutGoal)
        }

    @Test
    fun givenAnUnknownStoredThemeModeText_whenPreferencesRead_thenFallsBackToTheDefault() =
        runTest {
            dataSource.setThemeMode("sepia")

            assertEquals(UserPreferences.DEFAULT.themeMode, repository.observePreferences().first().themeMode)
        }

    @Test
    fun givenAnUnknownStoredDeviceUnitTextWithNoCachedAccount_whenUnitRead_thenFallsBackToKg() =
        runTest {
            dataSource.setUnitSystem("stone")

            assertEquals(UnitSystem.KG, repository.observePreferences().first().unitSystem)
        }

    private fun userEntity(unitSystem: String) =
        CurrentUserEntity(id = testUuid("1"), email = "alex@repz.app", displayName = "Alex", unitSystem = unitSystem)
}
