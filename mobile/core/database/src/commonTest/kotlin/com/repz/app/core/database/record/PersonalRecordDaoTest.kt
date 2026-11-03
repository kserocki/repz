package com.repz.app.core.database.record

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
import kotlin.time.Instant
import kotlin.uuid.Uuid

class PersonalRecordDaoTest {
    // Room needs a real dispatcher for its query/invalidation work, not the runTest scheduler.
    private val database =
        Room.inMemoryDatabaseBuilder<RepzDatabase>().buildRepzDatabase(Dispatchers.IO)
    private val personalRecordDao = database.personalRecordDao()

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun givenRecordsForTwoExercises_whenReplacingOne_thenOnlyThatExercisesRecordsChange() =
        runTest {
            val exerciseAId = testUuid("1")
            val exerciseBId = testUuid("2")
            personalRecordDao.replaceForExercises(listOf(exerciseAId), records = listOf(record(exerciseAId, "heaviest")))
            personalRecordDao.replaceForExercises(listOf(exerciseBId), records = listOf(record(exerciseBId, "heaviest")))

            personalRecordDao.replaceForExercises(
                listOf(exerciseAId),
                records = listOf(record(exerciseAId, "heaviest"), record(exerciseAId, "most_reps")),
            )

            personalRecordDao.observeForExercise(exerciseAId).test {
                assertEquals(2, awaitItem().size)
            }
            personalRecordDao.observeForExercise(exerciseBId).test {
                assertEquals(1, awaitItem().size)
            }
        }

    @Test
    fun givenAnExercisesRecords_whenReplacedAgainForTheSameExercise_thenOldRecordsAreGoneAndOnlyTheNewOnesRemain() =
        runTest {
            val exerciseId = testUuid("1")
            personalRecordDao.replaceForExercises(listOf(exerciseId), records = listOf(record(exerciseId, "heaviest")))

            personalRecordDao.replaceForExercises(listOf(exerciseId), records = listOf(record(exerciseId, "est_1rm")))

            personalRecordDao.observeAll().test {
                assertEquals(listOf("est_1rm"), awaitItem().map { it.type })
            }
        }

    private fun record(
        exerciseId: Uuid,
        type: String,
    ) = PersonalRecordEntity(
        exerciseId = exerciseId,
        type = type,
        weightGrams = 100_000,
        reps = 5,
        workoutId = testUuid("90"),
        setId = testUuid("91"),
        achievedAt = Instant.parse("2024-01-01T10:00:00Z"),
    )
}
