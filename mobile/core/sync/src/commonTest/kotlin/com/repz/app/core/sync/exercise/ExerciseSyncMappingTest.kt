package com.repz.app.core.sync.exercise

import com.repz.app.core.database.exercise.ExerciseEntity
import com.repz.app.core.network.dto.EquipmentDto
import com.repz.app.core.network.dto.MuscleDto
import com.repz.app.core.network.dto.exercise.ExercisePayloadDto
import com.repz.app.core.network.dto.exercise.ExerciseTranslationDto
import com.repz.app.core.network.dto.sync.SyncChangeDto
import com.repz.app.core.sync.testUuid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

private val UPDATED_AT = Instant.parse("2026-01-01T11:05:00.000Z")

private fun sampleCustomExercise(): ExerciseEntity =
    ExerciseEntity(
        id = testUuid("1"),
        name = "My curl variation",
        primaryMuscle = "biceps",
        secondaryMuscles = listOf("forearms", "shoulders"),
        equipment = "dumbbell",
        isCustom = true,
        imageUrl = null,
        license = null,
        author = null,
        sourceUrl = null,
        updatedAt = UPDATED_AT,
        serverVersion = null,
        isDeleted = false,
    )

class ExerciseSyncMappingTest {
    @Test
    fun givenACustomExercise_whenBuildingTheUpsertPayload_thenMapsItsVocabularyAndName() {
        val payload = checkNotNull(sampleCustomExercise().toCustomUpsertPayload())

        assertEquals("My curl variation", payload.name)
        assertEquals(MuscleDto.BICEPS, payload.primaryMuscle)
        assertEquals(listOf(MuscleDto.FOREARMS, MuscleDto.SHOULDERS), payload.secondaryMuscles)
        assertEquals(EquipmentDto.DUMBBELL, payload.equipment)
    }

    @Test
    fun givenAnUnrecognisedPrimaryMuscle_whenBuildingTheUpsertPayload_thenReturnsNull() {
        val corrupted = sampleCustomExercise().copy(primaryMuscle = "not-a-muscle")

        assertNull(corrupted.toCustomUpsertPayload())
    }

    @Test
    fun givenAnUnrecognisedSecondaryMuscle_whenBuildingTheUpsertPayload_thenReturnsNull() {
        val corrupted = sampleCustomExercise().copy(secondaryMuscles = listOf("not-a-muscle"))

        assertNull(corrupted.toCustomUpsertPayload())
    }

    @Test
    fun givenAPulledExercise_whenMappedToEntities_thenFieldsAndTranslationsRoundTrip() {
        val payload =
            ExercisePayloadDto(
                name = "Barbell bench press",
                primaryMuscle = MuscleDto.CHEST,
                secondaryMuscles = listOf(MuscleDto.TRICEPS, MuscleDto.SHOULDERS),
                equipment = EquipmentDto.BARBELL,
                isCustom = false,
                imageUrl = "https://example.com/bench.png",
                license = "CC-BY",
                author = "wger",
                sourceUrl = "https://example.com/source",
                translations =
                    listOf(
                        ExerciseTranslationDto("en", "Barbell bench press", "Lie back..."),
                        ExerciseTranslationDto("pl", "Wyciskanie sztangi", null),
                    ),
            )
        val change = SyncChangeDto.Exercise(testUuid("1"), version = 2, updatedAt = UPDATED_AT, payload = payload)

        val (entity, translations) = change.toEntities()

        assertEquals("chest", entity.primaryMuscle)
        assertEquals(listOf("triceps", "shoulders"), entity.secondaryMuscles)
        assertEquals("barbell", entity.equipment)
        assertEquals(false, entity.isCustom)
        assertEquals(2, entity.serverVersion)
        assertEquals(UPDATED_AT, entity.updatedAt)
        assertEquals(2, translations.size)
        assertEquals("pl", translations.last().locale)
        assertEquals(testUuid("1"), translations.last().exerciseId)
    }
}
