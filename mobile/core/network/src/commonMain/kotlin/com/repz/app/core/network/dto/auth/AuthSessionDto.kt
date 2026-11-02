package com.repz.app.core.network.dto.auth

import com.repz.app.core.network.dto.user.UserDto
import kotlinx.serialization.Serializable

/** `register`/`login`'s response: `AuthTokens` flattened together with the `User` (an OpenAPI `allOf`). */
@Serializable
data class AuthSessionDto(
    val user: UserDto,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
)
