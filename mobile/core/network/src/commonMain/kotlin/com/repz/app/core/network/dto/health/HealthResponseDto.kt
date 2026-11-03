package com.repz.app.core.network.dto.health

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponseDto(
    val status: String,
)
