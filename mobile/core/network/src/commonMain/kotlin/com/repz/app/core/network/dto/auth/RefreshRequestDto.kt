package com.repz.app.core.network.dto.auth

import kotlinx.serialization.Serializable

@Serializable
internal data class RefreshRequestDto(
    val refreshToken: String,
)
