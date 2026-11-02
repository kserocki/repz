package com.repz.app.core.network.dto.auth

import kotlinx.serialization.Serializable

@Serializable
internal data class AuthTokensDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
)
