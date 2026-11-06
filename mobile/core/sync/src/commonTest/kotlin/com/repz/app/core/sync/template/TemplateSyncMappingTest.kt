package com.repz.app.core.sync.template

import com.repz.app.core.database.template.TemplateAggregate
import com.repz.app.core.database.template.TemplateExerciseEntity
import com.repz.app.core.database.template.WorkoutTemplateEntity
import com.repz.app.core.network.dto.sync.SyncChangeDto
import com.repz.app.core.sync.testUuid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

private val UPDATED_AT = Instant.parse("2026-01-01T11:05:00.000Z")

private fun sampleAggregate(): TemplateAggregate =
    TemplateAggregate(
        template = WorkoutTemplateEntity(testUuid("1"), "Push day", "notes", UPDATED_AT, serverVersion = null, isDeleted = false),
        // Out of order on purpose: the stored position drives the push order, not this list's order.
        exercises =
            listOf(
                TemplateExerciseEntity(
                    testUuid("1"),
                    position = 1,
                    exerciseId = testUuid("11"),
                    targetSets = 3,
                    targetRepsMin = null,
                    targetRepsMax = null,
                    restSeconds = 90,
                ),
                TemplateExerciseEntity(
                    testUuid("1"),
                    position = 0,
                    exerciseId = testUuid("10"),
                    targetSets = 4,
                    targetRepsMin = 8,
                    targetRepsMax = 12,
                    restSeconds = 60,
                ),
            ),
    )

class TemplateSyncMappingTest {
    @Test
    fun givenATemplate_whenBuildingTheUpsertPayload_thenExercisesAreOrderedByPositionNotListOrder() {
        val payload = sampleAggregate().toUpsertPayload()

        assertEquals(listOf(testUuid("10"), testUuid("11")), payload.exercises.map { it.exerciseId })
    }

    @Test
    fun givenTargetRepsBothSet_whenBuildingTheUpsertPayload_thenMapsToATargetRepsRange() {
        val payload = sampleAggregate().toUpsertPayload()

        assertEquals(
            8,
            payload.exercises
                .first()
                .targetReps
                ?.min,
        )
        assertEquals(
            12,
            payload.exercises
                .first()
                .targetReps
                ?.max,
        )
    }

    @Test
    fun givenTargetRepsBothNull_whenBuildingTheUpsertPayload_thenMapsToNull() {
        val payload = sampleAggregate().toUpsertPayload()

        assertNull(payload.exercises.last().targetReps)
    }

    @Test
    fun givenAPayload_whenMappedBackToEntities_thenFieldsRoundTripAndPositionComesFromListOrder() {
        val payload = sampleAggregate().toUpsertPayload()
        val change = SyncChangeDto.Template(testUuid("1"), version = 3, updatedAt = UPDATED_AT, payload = payload)

        val (entity, exercises) = change.toEntities()

        assertEquals(WorkoutTemplateEntity(testUuid("1"), "Push day", "notes", UPDATED_AT, 3, false), entity)
        assertEquals(listOf(0, 1), exercises.map { it.position })
        assertEquals(8, exercises[0].targetRepsMin)
        assertEquals(12, exercises[0].targetRepsMax)
        assertNull(exercises[1].targetRepsMin)
        assertNull(exercises[1].targetRepsMax)
    }
}
