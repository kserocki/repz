package com.repz.app.core.network.dto.user

import com.repz.app.core.network.dto.UnitSystemDto
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class UserDto(
    val id: Uuid,
    val email: String,
    val displayName: String,
    val unitSystem: UnitSystemDto,
)
