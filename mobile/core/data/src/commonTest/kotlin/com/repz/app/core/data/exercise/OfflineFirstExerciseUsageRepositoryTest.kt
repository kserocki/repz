package com.repz.app.core.data.exercise

import androidx.room3.Room
import com.repz.app.core.data.testUuid
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.buildRepzDatabase
import com.repz.app.core.database.exercise.ExerciseEntity
import com.repz.app.core.database.workout.WorkoutEntity
import com.repz.app.core.database.workout.WorkoutExerciseEntity
import com.repz.app.core.domain.exercise.RecentExerciseUse
import com.repz.app.core.domain.model.ExerciseId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class OfflineFirstExerciseUsageRepositoryTest {
    // Room needs a real dispatcher for its query/invalidation work, not the runTest scheduler.
    private val database = Room.inMemoryDatabaseBuilder<RepzDatabase>().buildRepzDatabase(Dispatchers.IO)
    private val repository = OfflineFirstExerciseUsageRepository(database.exerciseUsageDao())

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun givenAFinishedWorkoutUsingAnExercise_whenRecentlyUsedExercisesObserved_thenTheRowMapsToDomain() =
        runTest {
            val exerciseId = testUuid("3")
            database.exerciseDao().saveSyncedExercise(
                ExerciseEntity(
                    id = exerciseId,
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
                ),
                emptyList(),
            )
            val startedAt = Instant.parse("2024-01-01T10:00:00Z")
            val workoutId = testUuid("1")
            database.workoutDao().saveSyncedWorkout(
                WorkoutEntity(
                    id = workoutId,
                    templateId = null,
                    name = "Push day",
                    notes = "",
                    startedAt = startedAt,
                    finishedAt = Instant.parse("2024-01-01T11:00:00Z"),
                    updatedAt = Instant.parse("2024-01-01T11:00:00Z"),
                    serverVersion = null,
                    isDeleted = false,
                ),
                listOf(
                    WorkoutExerciseEntity(id = testUuid("2"), workoutId = workoutId, exerciseId = exerciseId, position = 0, notes = ""),
                ),
                emptyList(),
            )

            val recent = repository.observeRecentlyUsedExercises(limit = 5).first()

            assertEquals(listOf(RecentExerciseUse(ExerciseId(exerciseId), startedAt)), recent)
        }
}
