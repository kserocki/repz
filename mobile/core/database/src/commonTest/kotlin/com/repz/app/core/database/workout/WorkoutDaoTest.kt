package com.repz.app.core.database.workout

import androidx.room3.Room
import app.cash.turbine.test
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.buildRepzDatabase
import com.repz.app.core.database.testUuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

class WorkoutDaoTest {
    // Room needs a real dispatcher for its query/invalidation work, not the runTest scheduler.
    private val database =
        Room.inMemoryDatabaseBuilder<RepzDatabase>().buildRepzDatabase(Dispatchers.IO)
    private val workoutDao = database.workoutDao()

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun givenSavedWorkout_whenRead_thenAggregateRoundTripsChildrenOrderedByPositionWithNullableFieldsPreserved() =
        runTest {
            val workoutId = testUuid("1")
            val exerciseAId = testUuid("2")
            val exerciseBId = testUuid("3")
            val setAId = testUuid("4")
            val setBId = testUuid("5")
            val templateId = testUuid("6")

            workoutDao.saveSyncedWorkout(
                workout(workoutId, templateId = templateId),
                exercises =
                    listOf(
                        workoutExercise(exerciseBId, workoutId, position = 1),
                        workoutExercise(exerciseAId, workoutId, position = 0),
                    ),
                sets =
                    listOf(
                        workoutSet(setBId, exerciseAId, position = 1, rpe = 8.5, completedAt = Instant.parse("2024-01-01T10:05:00Z")),
                        workoutSet(setAId, exerciseAId, position = 0, rpe = null, completedAt = null),
                    ),
            )

            val aggregate = workoutDao.findWorkoutIncludingDeleted(workoutId)

            assertEquals(templateId, aggregate?.workout?.templateId)
            val orderedExercises = aggregate?.exercises.orEmpty().sortedBy { it.exercise.position }
            assertEquals(listOf(exerciseAId, exerciseBId), orderedExercises.map { it.exercise.id })
            val orderedSets = orderedExercises.first().sets.sortedBy { it.position }
            assertEquals(listOf(setAId, setBId), orderedSets.map { it.id })
            assertNull(orderedSets[0].rpe)
            assertNull(orderedSets[0].completedAt)
            assertEquals(8.5, orderedSets[1].rpe)
            assertEquals(Instant.parse("2024-01-01T10:05:00Z"), orderedSets[1].completedAt)
        }

    @Test
    fun givenWorkoutWithChildren_whenResavedWithFewerExercisesAndSets_thenNoOrphansRemain() =
        runTest {
            val workoutId = testUuid("1")
            val exerciseAId = testUuid("2")
            val exerciseBId = testUuid("3")

            workoutDao.saveSyncedWorkout(
                workout(workoutId),
                exercises = listOf(workoutExercise(exerciseAId, workoutId, 0), workoutExercise(exerciseBId, workoutId, 1)),
                sets = listOf(workoutSet(testUuid("4"), exerciseAId, 0), workoutSet(testUuid("5"), exerciseBId, 0)),
            )

            workoutDao.saveSyncedWorkout(
                workout(workoutId),
                exercises = listOf(workoutExercise(exerciseAId, workoutId, 0)),
                sets = listOf(workoutSet(testUuid("4"), exerciseAId, 0)),
            )

            val aggregate = workoutDao.findWorkoutIncludingDeleted(workoutId)

            assertEquals(1, aggregate?.exercises?.size)
            assertEquals(
                1,
                aggregate
                    ?.exercises
                    ?.first()
                    ?.sets
                    ?.size,
            )
        }

    @Test
    fun givenWorkoutWithAServerVersion_whenSavedLocally_thenServerVersionIsKeptAndIsDeletedIsForcedFalse() =
        runTest {
            val workoutId = testUuid("1")
            workoutDao.saveSyncedWorkout(workout(workoutId, serverVersion = 5), exercises = emptyList(), sets = emptyList())

            workoutDao.saveLocalWorkout(
                workout(workoutId, serverVersion = null, isDeleted = true),
                exercises = emptyList(),
                sets = emptyList(),
            )

            val aggregate = workoutDao.findWorkoutIncludingDeleted(workoutId)
            assertEquals(5L, aggregate?.workout?.serverVersion)
            assertEquals(false, aggregate?.workout?.isDeleted)
        }

    @Test
    fun givenWorkoutSavedLocally_whenSavedSynced_thenServerVersionIsOverwritten() =
        runTest {
            val workoutId = testUuid("1")
            workoutDao.saveLocalWorkout(workout(workoutId), exercises = emptyList(), sets = emptyList())

            workoutDao.saveSyncedWorkout(workout(workoutId, serverVersion = 7), exercises = emptyList(), sets = emptyList())

            assertEquals(7L, workoutDao.findWorkoutIncludingDeleted(workoutId)?.workout?.serverVersion)
        }

    @Test
    fun givenAnActiveAndAFinishedWorkout_whenObserved_thenEachAppearsOnlyInItsOwnStream() =
        runTest {
            val activeId = testUuid("1")
            val finishedId = testUuid("2")
            workoutDao.saveSyncedWorkout(workout(activeId, finishedAt = null), emptyList(), emptyList())
            workoutDao.saveSyncedWorkout(
                workout(finishedId, finishedAt = Instant.parse("2024-01-02T00:00:00Z")),
                emptyList(),
                emptyList(),
            )

            workoutDao.observeActiveWorkout().test {
                assertEquals(activeId, awaitItem()?.workout?.id)
            }
            workoutDao.observeFinishedWorkouts().test {
                assertEquals(listOf(finishedId), awaitItem().map { it.workout.id })
            }
        }

    @Test
    fun givenATombstonedWorkout_whenObserved_thenHiddenFromObserversButFindIncludingDeletedReturnsItWithChildren() =
        runTest {
            val workoutId = testUuid("1")
            val exerciseId = testUuid("2")
            workoutDao.saveSyncedWorkout(workout(workoutId), listOf(workoutExercise(exerciseId, workoutId, 0)), emptyList())

            workoutDao.setDeleted(workoutId, isDeleted = true, updatedAt = Instant.parse("2024-01-02T00:00:00Z"))

            workoutDao.observeWorkout(workoutId).test {
                assertNull(awaitItem())
            }
            val aggregate = workoutDao.findWorkoutIncludingDeleted(workoutId)
            assertEquals(true, aggregate?.workout?.isDeleted)
            assertEquals(1, aggregate?.exercises?.size)

            workoutDao.deleteChildren(workoutId)

            assertTrue(
                workoutDao
                    .findWorkoutIncludingDeleted(workoutId)
                    ?.exercises
                    .orEmpty()
                    .isEmpty(),
            )
        }

    @Test
    fun givenAPurgedWorkout_whenItsChildrenAreCounted_thenNoneRemain() =
        runTest {
            val purgedWorkoutId = testUuid("1")
            val exerciseId = testUuid("2")
            val setId = testUuid("3")
            workoutDao.saveSyncedWorkout(
                workout(purgedWorkoutId),
                listOf(workoutExercise(exerciseId, purgedWorkoutId, 0)),
                listOf(workoutSet(setId, exerciseId, 0)),
            )

            workoutDao.purge(purgedWorkoutId)

            assertNull(workoutDao.findWorkoutIncludingDeleted(purgedWorkoutId))
            assertEquals(0, database.workoutRawDao().countExerciseRowsById(exerciseId))
            assertEquals(0, database.workoutRawDao().countSetRowsById(setId))
        }

    @Test
    fun givenAPurgedWorkoutsChildren_whenTheirIdsAreReusedByAnotherWorkout_thenTheInsertSucceedsBecauseCascadeDeletedThem() =
        runTest {
            val purgedWorkoutId = testUuid("1")
            val reusedExerciseId = testUuid("2")
            val reusedSetId = testUuid("3")
            workoutDao.saveSyncedWorkout(
                workout(purgedWorkoutId),
                listOf(workoutExercise(reusedExerciseId, purgedWorkoutId, 0)),
                listOf(workoutSet(reusedSetId, reusedExerciseId, 0)),
            )

            workoutDao.purge(purgedWorkoutId)
            assertNull(workoutDao.findWorkoutIncludingDeleted(purgedWorkoutId))

            val newWorkoutId = testUuid("4")
            workoutDao.saveSyncedWorkout(
                workout(newWorkoutId),
                listOf(workoutExercise(reusedExerciseId, newWorkoutId, 0)),
                listOf(workoutSet(reusedSetId, reusedExerciseId, 0)),
            )

            val aggregate = workoutDao.findWorkoutIncludingDeleted(newWorkoutId)
            assertEquals(listOf(reusedExerciseId), aggregate?.exercises?.map { it.exercise.id })
            assertEquals(
                listOf(reusedSetId),
                aggregate
                    ?.exercises
                    ?.first()
                    ?.sets
                    ?.map { it.id },
            )
        }

    @Test
    fun givenFinishedActiveAndDeletedWorkoutsForAnExercise_whenFindingByExercise_thenOnlyTheFinishedNonDeletedOneMatches() =
        runTest {
            val exerciseId = testUuid("1")
            val finishedId = testUuid("2")
            val activeId = testUuid("3")
            val deletedId = testUuid("4")
            val finishedAt = Instant.parse("2024-01-02T00:00:00Z")

            workoutDao.saveSyncedWorkout(
                workout(finishedId, finishedAt = finishedAt),
                listOf(workoutExercise(testUuid("5"), finishedId, 0, exerciseId)),
                emptyList(),
            )
            workoutDao.saveSyncedWorkout(
                workout(activeId, finishedAt = null),
                listOf(workoutExercise(testUuid("6"), activeId, 0, exerciseId)),
                emptyList(),
            )
            workoutDao.saveSyncedWorkout(
                workout(deletedId, finishedAt = finishedAt, isDeleted = true),
                listOf(workoutExercise(testUuid("7"), deletedId, 0, exerciseId)),
                emptyList(),
            )

            val found = workoutDao.findFinishedWorkoutsContainingExercises(listOf(exerciseId))

            assertEquals(listOf(finishedId), found.map { it.workout.id })
        }

    @Test
    fun givenFinishedActiveAndDeletedWorkouts_whenFindingFinishedWorkouts_thenOnlyTheFinishedNonDeletedOnesAreReturnedNewestFirst() =
        runTest {
            val olderFinishedId = testUuid("1")
            val newerFinishedId = testUuid("2")
            val activeId = testUuid("3")
            val deletedId = testUuid("4")

            workoutDao.saveSyncedWorkout(
                workout(olderFinishedId, finishedAt = Instant.parse("2024-01-01T12:00:00Z")),
                emptyList(),
                emptyList(),
            )
            workoutDao.saveSyncedWorkout(
                workout(newerFinishedId, finishedAt = Instant.parse("2024-01-02T12:00:00Z")),
                emptyList(),
                emptyList(),
            )
            workoutDao.saveSyncedWorkout(workout(activeId, finishedAt = null), emptyList(), emptyList())
            workoutDao.saveSyncedWorkout(
                workout(deletedId, finishedAt = Instant.parse("2024-01-03T12:00:00Z"), isDeleted = true),
                emptyList(),
                emptyList(),
            )

            val found = workoutDao.findFinishedWorkouts()

            assertEquals(listOf(newerFinishedId, olderFinishedId), found.map { it.workout.id })
        }

    private fun workout(
        id: Uuid,
        templateId: Uuid? = null,
        finishedAt: Instant? = Instant.parse("2024-01-01T11:00:00Z"),
        serverVersion: Long? = null,
        isDeleted: Boolean = false,
    ) = WorkoutEntity(
        id = id,
        templateId = templateId,
        name = "Push day",
        notes = "",
        startedAt = Instant.parse("2024-01-01T10:00:00Z"),
        finishedAt = finishedAt,
        updatedAt = Instant.parse("2024-01-01T11:00:00Z"),
        serverVersion = serverVersion,
        isDeleted = isDeleted,
    )

    private fun workoutExercise(
        id: Uuid,
        workoutId: Uuid,
        position: Int,
        exerciseId: Uuid = testUuid("999"),
    ) = WorkoutExerciseEntity(id = id, workoutId = workoutId, exerciseId = exerciseId, position = position, notes = "")

    private fun workoutSet(
        id: Uuid,
        workoutExerciseId: Uuid,
        position: Int,
        rpe: Double? = null,
        completedAt: Instant? = null,
    ) = WorkoutSetEntity(
        id = id,
        workoutExerciseId = workoutExerciseId,
        position = position,
        type = "normal",
        weightGrams = 20_000,
        reps = 8,
        rpe = rpe,
        completedAt = completedAt,
    )
}
