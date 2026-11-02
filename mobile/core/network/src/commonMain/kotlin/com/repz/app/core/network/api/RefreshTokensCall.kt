package com.repz.app.core.network.api

import com.repz.app.core.network.dto.auth.AuthTokensDto
import com.repz.app.core.network.dto.auth.RefreshRequestDto
import com.repz.app.core.network.result.NetworkResult
import com.repz.app.core.network.result.apiCall
import com.repz.app.core.network.session.AuthTokens
import com.repz.app.core.network.session.RefreshToken
import com.repz.app.core.network.session.toAuthTokens
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** Not part of [AuthApi]: only [com.repz.app.core.network.session.TokenRefresher] calls this, always over the public client. */
internal class RefreshTokensCall(
    private val publicClient: HttpClient,
) {
    suspend operator fun invoke(refreshToken: RefreshToken): NetworkResult<AuthTokens> =
        apiCall(
            decode = { it.body<AuthTokensDto>().toAuthTokens() },
            request = {
                publicClient.post("/auth/refresh") {
                    contentType(ContentType.Application.Json)
                    setBody(RefreshRequestDto(refreshToken.value))
                }
            },
        )
}
