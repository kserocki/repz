package com.repz.app.core.network.session

import com.repz.app.core.network.dto.auth.AuthSessionDto
import com.repz.app.core.network.dto.auth.AuthTokensDto
import kotlin.jvm.JvmInline

/** `toString()` never prints the value: a stray `"$accessToken"` in a log line must not leak a secret. */
@JvmInline
value class AccessToken(
    val value: String,
) {
    override fun toString(): String = "AccessToken(***)"
}

@JvmInline
value class RefreshToken(
    val value: String,
) {
    override fun toString(): String = "RefreshToken(***)"
}

data class AuthTokens(
    val accessToken: AccessToken,
    val refreshToken: RefreshToken,
)

fun AuthSessionDto.toAuthTokens(): AuthTokens = AuthTokens(AccessToken(accessToken), RefreshToken(refreshToken))

internal fun AuthTokensDto.toAuthTokens(): AuthTokens = AuthTokens(AccessToken(accessToken), RefreshToken(refreshToken))
