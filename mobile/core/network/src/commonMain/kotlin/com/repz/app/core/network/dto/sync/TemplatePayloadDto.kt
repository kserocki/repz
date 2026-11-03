package com.repz.app.core.network.dto.sync

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class TargetRepsDto(
    val min: Int,
    val max: Int,
)

@Serializable
data class TemplateExercisePayloadDto(
    val exerciseId: Uuid,
    val targetSets: Int,
    val targetReps: TargetRepsDto?,
    val restSeconds: Int,
)

/** List order is the order (ADR-0020): there is no separate `order` field. */
@Serializable
data class TemplatePayloadDto(
    val name: String,
    val notes: String = "",
    val exercises: List<TemplateExercisePayloadDto>,
)
