package com.repz.app.core.network.dto.sync

import com.repz.app.core.network.dto.SetTypeDto
import com.repz.app.core.network.serialization.WireDoubleSerializer
import com.repz.app.core.network.serialization.WireInstantSerializer
import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class WorkoutSetPayloadDto(
    val id: Uuid,
    val type: SetTypeDto,
    val weightGrams: Long,
    val reps: Int,
    @Serializable(with = WireDoubleSerializer::class) val rpe: Double?,
    @Serializable(with = WireInstantSerializer::class) val completedAt: Instant?,
)

/** List order is the order (ADR-0020): there is no separate `order` field. */
@Serializable
data class WorkoutExercisePayloadDto(
    val id: Uuid,
    val exerciseId: Uuid,
    val notes: String = "",
    val sets: List<WorkoutSetPayloadDto>,
)

@Serializable
data class WorkoutPayloadDto(
    val templateId: Uuid?,
    val name: String,
    val notes: String = "",
    @Serializable(with = WireInstantSerializer::class) val startedAt: Instant,
    @Serializable(with = WireInstantSerializer::class) val finishedAt: Instant,
    val exercises: List<WorkoutExercisePayloadDto>,
)
