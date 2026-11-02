package com.repz.app.core.network.dto.exercise

import com.repz.app.core.network.dto.EquipmentDto
import com.repz.app.core.network.dto.MuscleDto
import kotlinx.serialization.Serializable

@Serializable
data class ExerciseTranslationDto(
    val locale: String,
    val name: String,
    val instructions: String?,
)

/** Sync pull only: a catalog or a custom exercise, as the client sees it (mapping validates `imageUrl`/`sourceUrl` in REP-26). */
@Serializable
data class ExercisePayloadDto(
    val name: String,
    val primaryMuscle: MuscleDto,
    val secondaryMuscles: List<MuscleDto>,
    val equipment: EquipmentDto,
    val isCustom: Boolean,
    val imageUrl: String?,
    val license: String?,
    val author: String?,
    val sourceUrl: String?,
    val translations: List<ExerciseTranslationDto>,
)
