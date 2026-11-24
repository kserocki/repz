package com.repz.app.core.database.workout

import androidx.room3.Room
import app.cash.turbine.test
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.buildRepzDatabase
import com.repz.app.core.database.exercise.ExerciseEntity
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

    @Test
    fun givenAFinishedWorkoutUsingAnExercise_whenRecentlyUsedExercisesObserved_thenItAppearsWithTheWorkoutsStartedAt() =
        runTest {
            val exerciseId = testUuid("1")
            database.exerciseDao().saveSyncedExercise(exercise(exerciseId), emptyList())
            val startedAt = Instant.parse("2024-01-01T10:00:00Z")
            workoutDao.saveSyncedWorkout(
                workout(testUuid("2"), startedAt = startedAt, finishedAt = Instant.parse("2024-01-01T11:00:00Z")),
                listOf(workoutExercise(testUuid("3"), testUuid("2"), 0, exerciseId)),
                emptyList(),
            )

            workoutDao.observeRecentlyUsedExercises(limit = 5).test {
                assertEquals(listOf(RecentExerciseUseRow(exerciseId, startedAt)), awaitItem())
            }
        }

    @Test
    fun givenAnActiveWorkoutUsingAnExercise_whenRecentlyUsedExercisesObserved_thenItIsExcluded() =
        runTest {
            val exerciseId = testUuid("1")
            database.exerciseDao().saveSyncedExercise(exercise(exerciseId), emptyList())
            workoutDao.saveSyncedWorkout(
                workout(testUuid("2"), finishedAt = null),
                listOf(workoutExercise(testUuid("3"), testUuid("2"), 0, exerciseId)),
                emptyList(),
            )

            workoutDao.observeRecentlyUsedExercises(limit = 5).test {
                assertTrue(awaitItem().isEmpty())
            }
        }

    @Test
    fun givenADeletedFinishedWorkoutUsingAnExercise_whenRecentlyUsedExercisesObserved_thenItIsExcluded() =
        runTest {
            val exerciseId = testUuid("1")
            database.exerciseDao().saveSyncedExercise(exercise(exerciseId), emptyList())
            workoutDao.saveSyncedWorkout(
                workout(testUuid("2"), isDeleted = true),
                listOf(workoutExercise(testUuid("3"), testUuid("2"), 0, exerciseId)),
                emptyList(),
            )

            workoutDao.observeRecentlyUsedExercises(limit = 5).test {
                assertTrue(awaitItem().isEmpty())
            }
        }

    @Test
    fun givenAFinishedWorkoutUsingADeletedExercise_whenRecentlyUsedExercisesObserved_thenItIsExcluded() =
        runTest {
            val exerciseId = testUuid("1")
            database.exerciseDao().saveSyncedExercise(exercise(exerciseId), emptyList())
            database.exerciseDao().setDeleted(exerciseId, isDeleted = true, updatedAt = Instant.parse("2024-01-02T00:00:00Z"))
            workoutDao.saveSyncedWorkout(
                workout(testUuid("2")),
                listOf(workoutExercise(testUuid("3"), testUuid("2"), 0, exerciseId)),
                emptyList(),
            )

            workoutDao.observeRecentlyUsedExercises(limit = 5).test {
                assertTrue(awaitItem().isEmpty())
            }
        }

    @Test
    fun givenTheSameExerciseUsedInTwoFinishedWorkouts_whenRecentlyUsedExercisesObserved_thenTheLaterStartedAtWins() =
        runTest {
            val exerciseId = testUuid("1")
            database.exerciseDao().saveSyncedExercise(exercise(exerciseId), emptyList())
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

            workoutDao.observeRecentlyUsedExercises(limit = 5).test {
                assertEquals(listOf(RecentExerciseUseRow(exerciseId, newerStartedAt)), awaitItem())
            }
        }

    @Test
    fun givenExercisesLastUsedAtDifferentTimes_whenRecentlyUsedExercisesObserved_thenNewestFirst() =
        runTest {
            val olderExerciseId = testUuid("1")
            val newerExerciseId = testUuid("2")
            database.exerciseDao().saveSyncedExercise(exercise(olderExerciseId), emptyList())
            database.exerciseDao().saveSyncedExercise(exercise(newerExerciseId), emptyList())
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

            workoutDao.observeRecentlyUsedExercises(limit = 5).test {
                assertEquals(listOf(newerExerciseId, olderExerciseId), awaitItem().map { it.exerciseId })
            }
        }

    @Test
    fun givenTwoExercisesLastUsedAtTheSameTime_whenRecentlyUsedExercisesObserved_thenExerciseIdBreaksTheTieAscending() =
        runTest {
            val higherExerciseId = testUuid("2")
            val lowerExerciseId = testUuid("1")
            database.exerciseDao().saveSyncedExercise(exercise(higherExerciseId), emptyList())
            database.exerciseDao().saveSyncedExercise(exercise(lowerExerciseId), emptyList())
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

            workoutDao.observeRecentlyUsedExercises(limit = 5).test {
                assertEquals(listOf(lowerExerciseId, higherExerciseId), awaitItem().map { it.exerciseId })
            }
        }

    @Test
    fun givenMoreRecentlyUsedExercisesThanTheLimit_whenObserved_thenOnlyTheLimitIsReturned() =
        runTest {
            val exerciseIds = listOf(testUuid("1"), testUuid("2"), testUuid("3"))
            exerciseIds.forEach { database.exerciseDao().saveSyncedExercise(exercise(it), emptyList()) }
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

            workoutDao.observeRecentlyUsedExercises(limit = 2).test {
                assertEquals(2, awaitItem().size)
            }
        }

    private fun workout(
        id: Uuid,
        templateId: Uuid? = null,
        startedAt: Instant = Instant.parse("2024-01-01T10:00:00Z"),
        finishedAt: Instant? = Instant.parse("2024-01-01T11:00:00Z"),
        serverVersion: Long? = null,
        isDeleted: Boolean = false,
    ) = WorkoutEntity(
        id = id,
        templateId = templateId,
        name = "Push day",
        notes = "",
        startedAt = startedAt,
        finishedAt = finishedAt,
        updatedAt = Instant.parse("2024-01-01T11:00:00Z"),
        serverVersion = serverVersion,
        isDeleted = isDeleted,
    )

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
