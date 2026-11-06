package com.repz.app.core.sync.exercise

import com.repz.app.core.database.exercise.ExerciseEntity
import com.repz.app.core.database.exercise.ExerciseTranslationEntity
import com.repz.app.core.network.dto.exercise.CustomExercisePayloadDto
import com.repz.app.core.network.dto.exercise.ExerciseTranslationDto
import com.repz.app.core.network.dto.sync.SyncChangeDto
import com.repz.app.core.sync.vocabulary.WireVocabulary
import kotlin.uuid.Uuid

/** Push-only: a custom exercise's attribution fields never leave the client, so the payload has none. */
@Suppress("ReturnCount")
internal fun ExerciseEntity.toCustomUpsertPayload(): CustomExercisePayloadDto? {
    val primaryMuscleDto = WireVocabulary.muscleFromEntityValue(primaryMuscle) ?: return null
    val equipmentDto = WireVocabulary.equipmentFromEntityValue(equipment) ?: return null
    val secondaryMuscleDtos = secondaryMuscles.map { WireVocabulary.muscleFromEntityValue(it) ?: return null }
    return CustomExercisePayloadDto(
        name = name,
        primaryMuscle = primaryMuscleDto,
        secondaryMuscles = secondaryMuscleDtos,
        equipment = equipmentDto,
    )
}

/**
 * Maps one pulled exercise change (catalog or custom) to its entity shape, ready for
 * [com.repz.app.core.database.exercise.ExerciseDao.saveSyncedExercise].
 */
internal fun SyncChangeDto.Exercise.toEntities(): Pair<ExerciseEntity, List<ExerciseTranslationEntity>> {
    val entity =
        ExerciseEntity(
            id = id,
            name = payload.name,
            primaryMuscle = WireVocabulary.toEntityValue(payload.primaryMuscle),
            secondaryMuscles = payload.secondaryMuscles.map { WireVocabulary.toEntityValue(it) },
            equipment = WireVocabulary.toEntityValue(payload.equipment),
            isCustom = payload.isCustom,
            imageUrl = payload.imageUrl,
            license = payload.license,
            author = payload.author,
            sourceUrl = payload.sourceUrl,
            updatedAt = updatedAt,
            serverVersion = version,
            isDeleted = false,
        )
    val translations = payload.translations.map { it.toEntity(id) }
    return entity to translations
}

private fun ExerciseTranslationDto.toEntity(exerciseId: Uuid): ExerciseTranslationEntity =
    ExerciseTranslationEntity(
        exerciseId = exerciseId,
        locale = locale,
        name = name,
        instructions = instructions,
    )
