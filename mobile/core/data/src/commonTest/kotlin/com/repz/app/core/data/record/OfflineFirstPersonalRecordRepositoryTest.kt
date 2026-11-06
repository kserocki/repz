package com.repz.app.core.data.record

import androidx.room3.Room
import com.repz.app.core.data.testUuid
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.buildRepzDatabase
import com.repz.app.core.database.record.PersonalRecordEntity
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.model.WorkoutId
import com.repz.app.core.domain.model.WorkoutSetId
import com.repz.app.core.domain.record.PersonalRecord
import com.repz.app.core.domain.record.PersonalRecordType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class OfflineFirstPersonalRecordRepositoryTest {
    // Room needs a real dispatcher for its query/invalidation work, not the runTest scheduler.
    private val database = Room.inMemoryDatabaseBuilder<RepzDatabase>().buildRepzDatabase(Dispatchers.IO)
    private val personalRecordDao = database.personalRecordDao()
    private val repository = OfflineFirstPersonalRecordRepository(personalRecordDao)

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun givenRecordsForTwoExercises_whenReplacingOne_thenOnlyThatExercisesRecordsChange() =
        runTest {
            val exerciseA = ExerciseId(testUuid("1"))
            val exerciseB = ExerciseId(testUuid("2"))
            repository.replaceForExercises(setOf(exerciseA), listOf(record(exerciseA, PersonalRecordType.HEAVIEST)))
            repository.replaceForExercises(setOf(exerciseB), listOf(record(exerciseB, PersonalRecordType.HEAVIEST)))

            repository.replaceForExercises(
                setOf(exerciseA),
                listOf(record(exerciseA, PersonalRecordType.HEAVIEST), record(exerciseA, PersonalRecordType.MOST_REPS)),
            )

            assertEquals(2, repository.observeForExercise(exerciseA).first().size)
            assertEquals(1, repository.observeForExercise(exerciseB).first().size)
        }

    @Test
    fun givenRecordsAcrossExercises_whenReplacingAll_thenTheWholeTableIsReplaced() =
        runTest {
            val exerciseA = ExerciseId(testUuid("1"))
            val exerciseB = ExerciseId(testUuid("2"))
            repository.replaceForExercises(setOf(exerciseA), listOf(record(exerciseA, PersonalRecordType.HEAVIEST)))

            repository.replaceAll(listOf(record(exerciseB, PersonalRecordType.EST_1RM)))

            assertEquals(listOf(exerciseB), repository.observeAll().first().map { it.exerciseId })
        }

    @Test
    fun givenEveryRecordType_whenSavedAndRead_thenEveryFieldRoundTrips() =
        runTest {
            val exerciseId = ExerciseId(testUuid("1"))
            val records =
                PersonalRecordType.entries.map { type ->
                    record(exerciseId, type, weight = Weight.fromKilograms(100.0 + type.ordinal), reps = 3 + type.ordinal)
                }

            repository.replaceForExercises(setOf(exerciseId), records)

            assertEquals(records.toSet(), repository.observeForExercise(exerciseId).first().toSet())
        }

    @Test
    fun givenAStoredRecordWithAnUnknownType_whenObserved_thenDroppedFromTheList() =
        runTest {
            val exerciseId = testUuid("1")
            personalRecordDao.replaceForExercises(
                listOf(exerciseId),
                listOf(
                    PersonalRecordEntity(
                        exerciseId = exerciseId,
                        type = "bogus",
                        weightGrams = 100_000,
                        reps = 5,
                        workoutId = testUuid("2"),
                        setId = testUuid("3"),
                        achievedAt = Instant.parse("2024-01-01T10:00:00Z"),
                    ),
                ),
            )

            assertTrue(repository.observeAll().first().isEmpty())
            assertNull(repository.observeForExercise(ExerciseId(exerciseId)).first().firstOrNull())
        }

    private fun record(
        exerciseId: ExerciseId,
        type: PersonalRecordType,
        weight: Weight = Weight.fromKilograms(100.0),
        reps: Int = 5,
    ) = PersonalRecord(
        exerciseId = exerciseId,
        type = type,
        weight = weight,
        reps = reps,
        workoutId = WorkoutId(testUuid("90")),
        setId = WorkoutSetId(testUuid("91")),
        achievedAt = Instant.parse("2024-01-01T10:00:00Z"),
    )
}
