package com.repz.app.core.network.api

import com.repz.app.core.network.client.RepzHttpClients
import com.repz.app.core.network.dto.auth.AuthSessionDto
import com.repz.app.core.network.dto.auth.LoginRequestDto
import com.repz.app.core.network.dto.auth.LogoutRequestDto
import com.repz.app.core.network.dto.auth.RegisterRequestDto
import com.repz.app.core.network.result.NetworkResult
import com.repz.app.core.network.result.apiCall
import com.repz.app.core.network.session.RefreshToken
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.koin.core.annotation.Single

interface AuthApi {
    /** 201. */
    suspend fun register(request: RegisterRequestDto): NetworkResult<AuthSessionDto>

    /** 200. A wrong email/password surfaces as `Api(401, InvalidCredentials)`, never as a session timing out: login never refreshes. */
    suspend fun login(request: LoginRequestDto): NetworkResult<AuthSessionDto>

    /** 204. */
    suspend fun logout(refreshToken: RefreshToken): NetworkResult<Unit>
}

/** Always over the public client: register/login/logout never go through the bearer + refresh plugin. */
@Single
internal class KtorAuthApi(
    clients: RepzHttpClients,
) : AuthApi {
    private val publicClient = clients.public

    override suspend fun register(request: RegisterRequestDto): NetworkResult<AuthSessionDto> =
        apiCall(
            decode = { it.body() },
            request = {
                publicClient.post("/auth/register") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            },
        )

    override suspend fun login(request: LoginRequestDto): NetworkResult<AuthSessionDto> =
        apiCall(
            decode = { it.body() },
            request = {
                publicClient.post("/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            },
        )

    override suspend fun logout(refreshToken: RefreshToken): NetworkResult<Unit> =
        apiCall {
            publicClient.post("/auth/logout") {
                contentType(ContentType.Application.Json)
                setBody(LogoutRequestDto(refreshToken.value))
            }
        }
}
