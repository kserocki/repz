package com.repz.app.core.network.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class LogoutRequestDto(
    val refreshToken: String,
)
