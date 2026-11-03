package com.repz.app.core.network.dto.user

import com.repz.app.core.network.dto.UnitSystemDto
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode.NEVER
import kotlinx.serialization.Serializable

/**
 * At least one field is required (the server's strict schema rejects an explicit `null`, so an
 * absent field, not a null one, means "don't change this"): both default to `null` but are never
 * encoded at that default, so an omitted field is simply missing from the request body.
 */
@Serializable
data class UpdateProfileRequestDto(
    @EncodeDefault(NEVER) val displayName: String? = null,
    @EncodeDefault(NEVER) val unitSystem: UnitSystemDto? = null,
) {
    init {
        require(displayName != null || unitSystem != null) {
            "At least one of displayName or unitSystem is required"
        }
    }
}
