package com.repz.app.shared.sync

import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.record.RecalculateAllPersonalRecordsUseCase
import com.repz.app.core.domain.record.RecalculatePersonalRecordsUseCase
import com.repz.app.core.testing.record.FakePersonalRecordRepository
import com.repz.app.core.testing.transaction.FakeTransactionRunner
import com.repz.app.core.testing.workout.FakeWorkoutRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

/**
 * [PersonalRecordsRefresherAdapter] only has to forward to the right use case with the right id
 * mapping; the use cases' own tests cover what each one actually recalculates.
 */
class PersonalRecordsRefresherAdapterTest {
    private val workouts = FakeWorkoutRepository()
    private val records = FakePersonalRecordRepository()
    private val transactions = FakeTransactionRunner()
    private val adapter =
        PersonalRecordsRefresherAdapter(
            recalculateForExercises = RecalculatePersonalRecordsUseCase(workouts, records, transactions),
            recalculateAll = RecalculateAllPersonalRecordsUseCase(workouts, records, transactions),
        )

    @Test
    fun givenWireUuids_whenRefreshingForExercises_thenRecalculatePersonalRecordsUseCaseRunsWithMappedExerciseIds() =
        runTest {
            val exerciseUuid = TEST_EXERCISE_UUID

            adapter.refreshForExercises(setOf(exerciseUuid))

            assertEquals(setOf(ExerciseId(exerciseUuid)), records.replaceForExercisesCalls.single().first)
        }

    @Test
    fun givenAnEmptySet_whenRefreshingForExercises_thenNothingIsRecalculated() =
        runTest {
            adapter.refreshForExercises(emptySet())

            assertEquals(0, records.replaceForExercisesCalls.size)
        }

    @Test
    fun givenNoArguments_whenRefreshingAll_thenRecalculateAllPersonalRecordsUseCaseRuns() =
        runTest {
            adapter.refreshAll()

            assertEquals(1, records.replaceAllCalls.size)
        }

    private companion object {
        /** A fixed, literal id (Konsist I1: ids are minted only by the id generator). */
        val TEST_EXERCISE_UUID: Uuid = Uuid.parse("00000000-0000-7000-8000-000000000001")
    }
}
