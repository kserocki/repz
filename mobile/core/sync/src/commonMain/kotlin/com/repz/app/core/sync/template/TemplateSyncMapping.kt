package com.repz.app.core.sync.template

import com.repz.app.core.database.template.TemplateAggregate
import com.repz.app.core.database.template.TemplateExerciseEntity
import com.repz.app.core.database.template.WorkoutTemplateEntity
import com.repz.app.core.network.dto.sync.SyncChangeDto
import com.repz.app.core.network.dto.sync.TargetRepsDto
import com.repz.app.core.network.dto.sync.TemplateExercisePayloadDto
import com.repz.app.core.network.dto.sync.TemplatePayloadDto

/** Children are sorted by their stored position (ADR-0020: list order is the order, no separate `order` field). */
internal fun TemplateAggregate.toUpsertPayload(): TemplatePayloadDto =
    TemplatePayloadDto(
        name = template.name,
        notes = template.notes,
        exercises = exercises.sortedBy { it.position }.map { it.toPayload() },
    )

private fun TemplateExerciseEntity.toPayload(): TemplateExercisePayloadDto =
    TemplateExercisePayloadDto(
        exerciseId = exerciseId,
        targetSets = targetSets,
        targetReps = targetRepsOrNull(targetRepsMin, targetRepsMax),
        restSeconds = restSeconds,
    )

private fun targetRepsOrNull(
    min: Int?,
    max: Int?,
): TargetRepsDto? = if (min != null && max != null) TargetRepsDto(min, max) else null

/** Maps one pulled template change to its entity shape, ready for [com.repz.app.core.database.template.TemplateDao.saveSyncedTemplate]. */
internal fun SyncChangeDto.Template.toEntities(): Pair<WorkoutTemplateEntity, List<TemplateExerciseEntity>> {
    val entity =
        WorkoutTemplateEntity(
            id = id,
            name = payload.name,
            notes = payload.notes,
            updatedAt = updatedAt,
            serverVersion = version,
            isDeleted = false,
        )
    val exercises =
        payload.exercises.mapIndexed { index, exercisePayload ->
            TemplateExerciseEntity(
                templateId = id,
                position = index,
                exerciseId = exercisePayload.exerciseId,
                targetSets = exercisePayload.targetSets,
                targetRepsMin = exercisePayload.targetReps?.min,
                targetRepsMax = exercisePayload.targetReps?.max,
                restSeconds = exercisePayload.restSeconds,
            )
        }
    return entity to exercises
}
