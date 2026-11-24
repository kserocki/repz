package com.repz.app.core.database.exercise

import androidx.room3.Room
import app.cash.turbine.test
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.buildRepzDatabase
import com.repz.app.core.database.testUuid
import com.repz.app.core.database.workout.WorkoutEntity
import com.repz.app.core.database.workout.WorkoutExerciseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

class ExerciseUsageDaoTest {
    // Room needs a real dispatcher for its query/invalidation work, not the runTest scheduler.
    private val database =
        Room.inMemoryDatabaseBuilder<RepzDatabase>().buildRepzDatabase(Dispatchers.IO)
    private val exerciseUsageDao = database.exerciseUsageDao()
    private val workoutDao = database.workoutDao()
    private val exerciseDao = database.exerciseDao()

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun givenAFinishedWorkoutUsingAnExercise_whenRecentlyUsedExercisesObserved_thenItAppearsWithTheWorkoutsStartedAt() =
        runTest {
            val exerciseId = testUuid("1")
            exerciseDao.saveSyncedExercise(exercise(exerciseId), emptyList())
            val startedAt = Instant.parse("2024-01-01T10:00:00Z")
            workoutDao.saveSyncedWorkout(
                workout(testUuid("2"), startedAt = startedAt, finishedAt = Instant.parse("2024-01-01T11:00:00Z")),
                listOf(workoutExercise(testUuid("3"), testUuid("2"), 0, exerciseId)),
                emptyList(),
            )

            exerciseUsageDao.observeRecentlyUsedExercises(limit = 5).test {
                assertEquals(listOf(RecentExerciseUseRow(exerciseId, startedAt)), awaitItem())
            }
        }

    @Test
    fun givenAnActiveWorkoutUsingAnExercise_whenRecentlyUsedExercisesObserved_thenItIsExcluded() =
        runTest {
            val exerciseId = testUuid("1")
            exerciseDao.saveSyncedExercise(exercise(exerciseId), emptyList())
            workoutDao.saveSyncedWorkout(
                workout(testUuid("2"), finishedAt = null),
                listOf(workoutExercise(testUuid("3"), testUuid("2"), 0, exerciseId)),
                emptyList(),
            )

            exerciseUsageDao.observeRecentlyUsedExercises(limit = 5).test {
                assertTrue(awaitItem().isEmpty())
            }
        }

    @Test
    fun givenADeletedFinishedWorkoutUsingAnExercise_whenRecentlyUsedExercisesObserved_thenItIsExcluded() =
        runTest {
            val exerciseId = testUuid("1")
            exerciseDao.saveSyncedExercise(exercise(exerciseId), emptyList())
            workoutDao.saveSyncedWorkout(
                workout(testUuid("2"), isDeleted = true),
                listOf(workoutExercise(testUuid("3"), testUuid("2"), 0, exerciseId)),
                emptyList(),
            )

            exerciseUsageDao.observeRecentlyUsedExercises(limit = 5).test {
                assertTrue(awaitItem().isEmpty())
            }
        }

    @Test
    fun givenAFinishedWorkoutUsingADeletedExercise_whenRecentlyUsedExercisesObserved_thenItIsExcluded() =
        runTest {
            val exerciseId = testUuid("1")
            exerciseDao.saveSyncedExercise(exercise(exerciseId), emptyList())
            exerciseDao.setDeleted(exerciseId, isDeleted = true, updatedAt = Instant.parse("2024-01-02T00:00:00Z"))
            workoutDao.saveSyncedWorkout(
                workout(testUuid("2")),
                listOf(workoutExercise(testUuid("3"), testUuid("2"), 0, exerciseId)),
                emptyList(),
            )

            exerciseUsageDao.observeRecentlyUsedExercises(limit = 5).test {
                assertTrue(awaitItem().isEmpty())
            }
        }

    @Test
    fun givenTheSameExerciseUsedInTwoFinishedWorkouts_whenRecentlyUsedExercisesObserved_thenTheLaterStartedAtWins() =
        runTest {
            val exerciseId = testUuid("1")
            exerciseDao.saveSyncedExercise(exercise(exerciseId), emptyList())
            val olderStartedAt = Instant.parse("2024-01-01T10:00:00Z")
            val newerStartedAt = Instant.parse("2024-01-03T10:00:00Z")
            workoutDao.saveSyncedWorkout(
                workout(testUuid("2"), startedAt = olderStartedAt, finishedAt = Instant.parse("2024-01-01T11:00:00Z")),
                listOf(workoutExercise(testUuid("3"), testUuid("2"), 0, exerciseId)),
                emptyList(),
            )
            workoutDao.saveSyncedWorkout(
                workout(testUuid("4"), startedAt = newerStartedAt, finishedAt = Instant.parse("2024-01-03T11:00:00Z")),
                listOf(workoutExercise(testUuid("5"), testUuid("4"), 0, exerciseId)),
                emptyList(),
            )

            exerciseUsageDao.observeRecentlyUsedExercises(limit = 5).test {
                assertEquals(listOf(RecentExerciseUseRow(exerciseId, newerStartedAt)), awaitItem())
            }
        }

    @Test
    fun givenExercisesLastUsedAtDifferentTimes_whenRecentlyUsedExercisesObserved_thenNewestFirst() =
        runTest {
            val olderExerciseId = testUuid("1")
            val newerExerciseId = testUuid("2")
            exerciseDao.saveSyncedExercise(exercise(olderExerciseId), emptyList())
            exerciseDao.saveSyncedExercise(exercise(newerExerciseId), emptyList())
            workoutDao.saveSyncedWorkout(
                workout(
                    testUuid("3"),
                    startedAt = Instant.parse("2024-01-01T10:00:00Z"),
                    finishedAt = Instant.parse("2024-01-01T11:00:00Z"),
                ),
                listOf(workoutExercise(testUuid("4"), testUuid("3"), 0, olderExerciseId)),
                emptyList(),
            )
            workoutDao.saveSyncedWorkout(
                workout(
                    testUuid("5"),
                    startedAt = Instant.parse("2024-01-02T10:00:00Z"),
                    finishedAt = Instant.parse("2024-01-02T11:00:00Z"),
                ),
                listOf(workoutExercise(testUuid("6"), testUuid("5"), 0, newerExerciseId)),
                emptyList(),
            )

            exerciseUsageDao.observeRecentlyUsedExercises(limit = 5).test {
                assertEquals(listOf(newerExerciseId, olderExerciseId), awaitItem().map { it.exerciseId })
            }
        }

    @Test
    fun givenTwoExercisesLastUsedAtTheSameTime_whenRecentlyUsedExercisesObserved_thenExerciseIdBreaksTheTieAscending() =
        runTest {
            val higherExerciseId = testUuid("2")
            val lowerExerciseId = testUuid("1")
            exerciseDao.saveSyncedExercise(exercise(higherExerciseId), emptyList())
            exerciseDao.saveSyncedExercise(exercise(lowerExerciseId), emptyList())
            val startedAt = Instant.parse("2024-01-01T10:00:00Z")
            workoutDao.saveSyncedWorkout(
                workout(testUuid("3"), startedAt = startedAt, finishedAt = Instant.parse("2024-01-01T11:00:00Z")),
                listOf(workoutExercise(testUuid("4"), testUuid("3"), 0, higherExerciseId)),
                emptyList(),
            )
            workoutDao.saveSyncedWorkout(
                workout(testUuid("5"), startedAt = startedAt, finishedAt = Instant.parse("2024-01-01T11:00:00Z")),
                listOf(workoutExercise(testUuid("6"), testUuid("5"), 0, lowerExerciseId)),
                emptyList(),
            )

            exerciseUsageDao.observeRecentlyUsedExercises(limit = 5).test {
                assertEquals(listOf(lowerExerciseId, higherExerciseId), awaitItem().map { it.exerciseId })
            }
        }

    @Test
    fun givenMoreRecentlyUsedExercisesThanTheLimit_whenObserved_thenOnlyTheLimitIsReturned() =
        runTest {
            val exerciseIds = listOf(testUuid("1"), testUuid("2"), testUuid("3"))
            exerciseIds.forEach { exerciseDao.saveSyncedExercise(exercise(it), emptyList()) }
            exerciseIds.forEachIndexed { index, exerciseId ->
                val workoutId = testUuid((10 + index).toString())
                workoutDao.saveSyncedWorkout(
                    workout(
                        workoutId,
                        startedAt = Instant.parse("2024-01-0${index + 1}T10:00:00Z"),
                        finishedAt = Instant.parse("2024-01-0${index + 1}T11:00:00Z"),
                    ),
                    listOf(workoutExercise(testUuid((20 + index).toString()), workoutId, 0, exerciseId)),
                    emptyList(),
                )
            }

            exerciseUsageDao.observeRecentlyUsedExercises(limit = 2).test {
                assertEquals(2, awaitItem().size)
            }
        }

    private fun exercise(id: Uuid) =
        ExerciseEntity(
            id = id,
            name = "Bench press",
            primaryMuscle = "chest",
            secondaryMuscles = emptyList(),
            equipment = "barbell",
            isCustom = false,
            imageUrl = null,
            license = null,
            author = null,
            sourceUrl = null,
            updatedAt = Instant.parse("2024-01-01T11:00:00Z"),
            serverVersion = null,
            isDeleted = false,
        )

    private fun workout(
        id: Uuid,
        templateId: Uuid? = null,
        startedAt: Instant = Instant.parse("2024-01-01T10:00:00Z"),
        finishedAt: Instant? = Instant.parse("2024-01-01T11:00:00Z"),
        isDeleted: Boolean = false,
    ) = WorkoutEntity(
        id = id,
        templateId = templateId,
        name = "Push day",
        notes = "",
        startedAt = startedAt,
        finishedAt = finishedAt,
        updatedAt = Instant.parse("2024-01-01T11:00:00Z"),
        serverVersion = null,
        isDeleted = isDeleted,
    )

    private fun workoutExercise(
        id: Uuid,
        workoutId: Uuid,
        position: Int,
        exerciseId: Uuid,
    ) = WorkoutExerciseEntity(id = id, workoutId = workoutId, exerciseId = exerciseId, position = position, notes = "")
}
