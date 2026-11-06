package com.repz.app.core.data.template

import co.touchlab.kermit.Logger
import com.repz.app.core.database.template.TemplateAggregate
import com.repz.app.core.database.template.TemplateExerciseEntity
import com.repz.app.core.database.template.WorkoutTemplateEntity
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.TemplateExercise
import com.repz.app.core.domain.model.WorkoutTemplate
import com.repz.app.core.domain.model.WorkoutTemplateId
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** A corrupt row (R5, half-null target reps) is logged and dropped: `null` here, filtered out of a list by the caller. */
internal fun TemplateAggregate.toDomainOrNull(logger: Logger): WorkoutTemplate? {
    val orderedExercises = exercises.sortedBy { it.position }.map { it.toDomainOrNull(logger) }
    return if (orderedExercises.any { it == null }) {
        null
    } else {
        WorkoutTemplate(
            id = WorkoutTemplateId(template.id),
            name = template.name,
            notes = template.notes,
            exercises = orderedExercises.filterNotNull(),
        )
    }
}

internal fun List<TemplateAggregate>.toDomainList(logger: Logger): List<WorkoutTemplate> = mapNotNull { it.toDomainOrNull(logger) }

private fun TemplateExerciseEntity.toDomainOrNull(logger: Logger): TemplateExercise? {
    // Local vals, not the entity's own properties: a cross-module property can't be smart-cast.
    val targetRepsMin = targetRepsMin
    val targetRepsMax = targetRepsMax
    val isHalfNullTargetReps = (targetRepsMin == null) != (targetRepsMax == null)
    if (isHalfNullTargetReps) {
        logger.e { "Half-null target reps for template exercise ($templateId, $position)" }
    }
    return if (isHalfNullTargetReps) {
        null
    } else {
        TemplateExercise(
            exerciseId = ExerciseId(exerciseId),
            targetSets = targetSets,
            targetReps = if (targetRepsMin != null && targetRepsMax != null) targetRepsMin..targetRepsMax else null,
            rest = restSeconds.seconds,
        )
    }
}

/** [serverVersion]/[isDeleted] are placeholders: [com.repz.app.core.database.template.TemplateDao.saveLocalTemplate] overwrites both. */
internal fun WorkoutTemplate.toEntity(updatedAt: Instant): WorkoutTemplateEntity =
    WorkoutTemplateEntity(id = id.value, name = name, notes = notes, updatedAt = updatedAt, serverVersion = null, isDeleted = false)

internal fun WorkoutTemplate.toExerciseEntities(): List<TemplateExerciseEntity> =
    exercises.mapIndexed { index, exercise ->
        TemplateExerciseEntity(
            templateId = id.value,
            position = index,
            exerciseId = exercise.exerciseId.value,
            targetSets = exercise.targetSets,
            targetRepsMin = exercise.targetReps?.first,
            targetRepsMax = exercise.targetReps?.last,
            restSeconds = exercise.rest.inWholeSeconds.toInt(),
        )
    }
