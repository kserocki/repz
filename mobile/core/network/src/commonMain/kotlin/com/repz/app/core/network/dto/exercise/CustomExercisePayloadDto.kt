package com.repz.app.core.network.dto.exercise

import com.repz.app.core.network.dto.EquipmentDto
import com.repz.app.core.network.dto.MuscleDto
import kotlinx.serialization.Serializable

/** Sync push only: a user-authored exercise. */
@Serializable
data class CustomExercisePayloadDto(
    val name: String,
    val primaryMuscle: MuscleDto,
    val secondaryMuscles: List<MuscleDto>,
    val equipment: EquipmentDto,
)
