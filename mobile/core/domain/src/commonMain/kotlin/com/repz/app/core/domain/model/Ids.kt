package com.repz.app.core.domain.model

import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

/**
 * One value class per aggregate so the compiler rejects passing, say, a [WorkoutId] where an
 * [ExerciseId] is expected. Callers build them as `WorkoutId(idGenerator.newId())`.
 */
@JvmInline
value class UserId(
    val value: Uuid,
)

@JvmInline
value class ExerciseId(
    val value: Uuid,
)

@JvmInline
value class WorkoutTemplateId(
    val value: Uuid,
)

@JvmInline
value class WorkoutId(
    val value: Uuid,
)

@JvmInline
value class WorkoutExerciseId(
    val value: Uuid,
)

@JvmInline
value class WorkoutSetId(
    val value: Uuid,
)
