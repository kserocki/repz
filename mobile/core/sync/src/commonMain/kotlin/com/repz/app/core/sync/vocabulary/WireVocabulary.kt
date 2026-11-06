package com.repz.app.core.sync.vocabulary

import com.repz.app.core.network.dto.EquipmentDto
import com.repz.app.core.network.dto.MuscleDto
import com.repz.app.core.network.dto.SetTypeDto

/**
 * Converts a wire vocabulary enum to and from the lowercase snake_case string Room stores
 * unchanged (ADR-0023: "`core:sync` writes pulled values unchanged and `core:data` maps them to
 * the domain enums"). The entity string is derived from each enum's `@SerialName` via its
 * generated serializer's descriptor, so no literal is duplicated between the DTOs and this file.
 */
internal object WireVocabulary {
    fun toEntityValue(value: SetTypeDto): String = SetTypeDto.serializer().descriptor.getElementName(value.ordinal)

    fun toEntityValue(value: MuscleDto): String = MuscleDto.serializer().descriptor.getElementName(value.ordinal)

    fun toEntityValue(value: EquipmentDto): String = EquipmentDto.serializer().descriptor.getElementName(value.ordinal)

    /** Null when [entityValue] isn't one of [SetTypeDto]'s wire strings - a bug, since this value came from this app's own write. */
    fun setTypeFromEntityValue(entityValue: String): SetTypeDto? = SetTypeDto.entries.find { toEntityValue(it) == entityValue }

    fun muscleFromEntityValue(entityValue: String): MuscleDto? = MuscleDto.entries.find { toEntityValue(it) == entityValue }

    fun equipmentFromEntityValue(entityValue: String): EquipmentDto? = EquipmentDto.entries.find { toEntityValue(it) == entityValue }
}
