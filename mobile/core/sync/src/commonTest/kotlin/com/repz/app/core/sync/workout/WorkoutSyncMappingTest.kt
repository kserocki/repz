package com.repz.app.core.sync.workout

import com.repz.app.core.database.workout.WorkoutAggregate
import com.repz.app.core.database.workout.WorkoutEntity
import com.repz.app.core.database.workout.WorkoutExerciseEntity
import com.repz.app.core.database.workout.WorkoutExerciseWithSets
import com.repz.app.core.database.workout.WorkoutSetEntity
import com.repz.app.core.network.dto.sync.SyncChangeDto
import com.repz.app.core.sync.testUuid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

private val STARTED_AT = Instant.parse("2026-01-01T10:00:00.000Z")
private val FINISHED_AT = Instant.parse("2026-01-01T11:00:00.000Z")
private val UPDATED_AT = Instant.parse("2026-01-01T11:05:00.000Z")

private fun sampleAggregate(): WorkoutAggregate {
    val workoutId = testUuid("1")
    val exerciseAId = testUuid("10")
    val exerciseBId = testUuid("11")
    val setA1 = testUuid("20")
    val setA2 = testUuid("21")
    val setB1 = testUuid("22")

    return WorkoutAggregate(
        workout =
            WorkoutEntity(
                id = workoutId,
                templateId = testUuid("2"),
                name = "Push day",
                notes = "felt strong",
                startedAt = STARTED_AT,
                finishedAt = FINISHED_AT,
                updatedAt = UPDATED_AT,
                serverVersion = null,
                isDeleted = false,
            ),
        // Out of order on purpose: the stored position drives the push order, not this list's order.
        exercises =
            listOf(
                WorkoutExerciseWithSets(
                    exercise = WorkoutExerciseEntity(exerciseBId, workoutId, testUuid("31"), position = 1, notes = ""),
                    sets = listOf(WorkoutSetEntity(setB1, exerciseBId, 0, "normal", 20000, 10, null, null)),
                ),
                WorkoutExerciseWithSets(
                    exercise = WorkoutExerciseEntity(exerciseAId, workoutId, testUuid("30"), position = 0, notes = "warm up first"),
                    sets =
                        listOf(
                            WorkoutSetEntity(setA2, exerciseAId, 1, "normal", 60000, 5, 8.5, FINISHED_AT),
                            WorkoutSetEntity(setA1, exerciseAId, 0, "warmup", 40000, 8, null, STARTED_AT),
                        ),
                ),
            ),
    )
}

class WorkoutSyncMappingTest {
    @Test
    fun givenAFinishedWorkout_whenBuildingTheUpsertPayload_thenChildrenAreOrderedByPositionNotListOrder() {
        val payload = checkNotNull(sampleAggregate().toUpsertPayload())

        assertEquals(listOf(testUuid("30"), testUuid("31")), payload.exercises.map { it.exerciseId })
        assertEquals(
            listOf(testUuid("20"), testUuid("21")),
            payload.exercises
                .first()
                .sets
                .map { it.id },
        )
    }

    @Test
    fun givenAPayload_whenMappedBackToEntities_thenFieldsRoundTripAndPositionComesFromListOrder() {
        val payload = checkNotNull(sampleAggregate().toUpsertPayload())
        val change = SyncChangeDto.Workout(testUuid("1"), version = 7, updatedAt = UPDATED_AT, payload = payload)

        val (entity, exercises, sets) = change.toEntities()

        assertEquals(
            WorkoutEntity(testUuid("1"), testUuid("2"), "Push day", "felt strong", STARTED_AT, FINISHED_AT, UPDATED_AT, 7, false),
            entity,
        )
        assertEquals(listOf(0, 1), exercises.map { it.position })
        val firstExerciseSets = sets.filter { it.workoutExerciseId == exercises[0].id }
        assertEquals(listOf(0, 1), firstExerciseSets.map { it.position })
    }

    @Test
    fun givenASetWithAnUnrecognisedStoredType_whenBuildingTheUpsertPayload_thenReturnsNull() {
        val aggregate = sampleAggregate()
        val corrupted =
            aggregate.copy(
                exercises = aggregate.exercises.map { it.copy(sets = it.sets.map { set -> set.copy(type = "not-a-type") }) },
            )

        assertNull(corrupted.toUpsertPayload())
    }

    @Test
    fun givenVocabularyValues_whenRoundTripped_thenSurviveBothWays() {
        val payload = checkNotNull(sampleAggregate().toUpsertPayload())
        val change = SyncChangeDto.Workout(testUuid("1"), version = 1, updatedAt = UPDATED_AT, payload = payload)

        val (_, _, sets) = change.toEntities()

        assertEquals(setOf("warmup", "normal"), sets.map { it.type }.toSet())
    }
}
