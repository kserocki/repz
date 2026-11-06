package com.repz.app.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// One fixed, test-unique path per test method (§3.6 pitfall: only one active DataStore per file per
// process). Konsist I1 forbids Uuid.random in tests, so these are plain literal names instead.
private val noStoredValuesPath = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "repz-user-preferences-no-stored-values-test.preferences_pb"
private val roundTripPath = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "repz-user-preferences-round-trip-test.preferences_pb"
private val unitSystemRemovedPath = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "repz-user-preferences-unit-removed-test.preferences_pb"
private val survivesNewInstancePath =
    FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "repz-user-preferences-survives-new-instance-test.preferences_pb"

private val allTestPaths = listOf(noStoredValuesPath, roundTripPath, unitSystemRemovedPath, survivesNewInstancePath)

/** Exercises [UserPreferencesDataSource] against a real, file-backed `DataStore`. */
class UserPreferencesDataSourceTest {
    @BeforeTest
    fun deleteStaleFiles() = deleteTestFiles()

    @AfterTest
    fun deleteFilesAfterTest() = deleteTestFiles()

    @Test
    fun givenNoStoredValues_whenPreferencesRead_thenEveryFieldIsNull() =
        runTest {
            val dataSource = UserPreferencesDataSource(newDataStore(noStoredValuesPath))

            val stored = dataSource.preferences.first()

            assertEquals(StoredUserPreferences(null, null, null, null, null, null), stored)
        }

    @Test
    fun givenEverySetterCalled_whenPreferencesRead_thenEveryFieldRoundTrips() =
        runTest {
            val dataSource = UserPreferencesDataSource(newDataStore(roundTripPath))

            dataSource.setUnitSystem("kg")
            dataSource.setThemeMode("dark")
            dataSource.setDynamicColorEnabled(true)
            dataSource.setDefaultRestSeconds(90)
            dataSource.setHapticsEnabled(false)
            dataSource.setWeeklyWorkoutGoal(5)

            val stored = dataSource.preferences.first()

            assertEquals(
                StoredUserPreferences(
                    unitSystem = "kg",
                    themeMode = "dark",
                    dynamicColorEnabled = true,
                    defaultRestSeconds = 90,
                    hapticsEnabled = false,
                    weeklyWorkoutGoal = 5,
                ),
                stored,
            )
        }

    @Test
    fun givenUnitSystemStored_whenSetUnitSystemNull_thenKeyIsRemoved() =
        runTest {
            val dataSource = UserPreferencesDataSource(newDataStore(unitSystemRemovedPath))
            dataSource.setUnitSystem("lb")

            dataSource.setUnitSystem(null)

            assertNull(dataSource.preferences.first().unitSystem)
        }

    /**
     * The first `DataStore` is built directly (not through [createRepzPreferencesDataStore]) so the
     * test can hold and cancel its scope: only that releases DataStore's own "one active instance
     * per file" lock, letting the second, production-path instance open the same file.
     */
    @Test
    fun givenValuesWrittenByOneDataStoreInstance_whenReadByANewInstanceOnTheSameFile_thenValuesSurvive() =
        runTest {
            val firstScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            val firstDataSource =
                UserPreferencesDataSource(
                    PreferenceDataStoreFactory.createWithPath(
                        scope = firstScope,
                        produceFile = { survivesNewInstancePath },
                    ),
                )
            firstDataSource.setWeeklyWorkoutGoal(6)
            firstScope.coroutineContext.job.cancelAndJoin()

            val secondDataSource = UserPreferencesDataSource(newDataStore(survivesNewInstancePath))

            assertEquals(6, secondDataSource.preferences.first().weeklyWorkoutGoal)
        }

    private fun deleteTestFiles() {
        allTestPaths.forEach { path -> FileSystem.SYSTEM.delete(path, mustExist = false) }
    }
}

private fun newDataStore(path: Path) = createRepzPreferencesDataStore(Dispatchers.Default) { path.toString() }
