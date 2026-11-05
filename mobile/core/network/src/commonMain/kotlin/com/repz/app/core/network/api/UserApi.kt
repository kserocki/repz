package com.repz.app.core.network.api

import com.repz.app.core.network.client.RepzHttpClients
import com.repz.app.core.network.dto.user.UpdateProfileRequestDto
import com.repz.app.core.network.dto.user.UserDto
import com.repz.app.core.network.result.NetworkResult
import com.repz.app.core.network.result.apiCall
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.koin.core.annotation.Single

interface UserApi {
    suspend fun getMe(): NetworkResult<UserDto>

    /** A 400 envelope error is `Api(400, ValidationError)` (e.g. both fields absent). */
    suspend fun updateMe(request: UpdateProfileRequestDto): NetworkResult<UserDto>
}

@Single
internal class KtorUserApi(
    private val clients: RepzHttpClients,
) : UserApi {
    override suspend fun getMe(): NetworkResult<UserDto> =
        apiCall(
            sessionManager = clients.sessionManager,
            decode = { it.body() },
            request = { clients.authenticated.get("/me") },
        )

    override suspend fun updateMe(request: UpdateProfileRequestDto): NetworkResult<UserDto> =
        apiCall(
            sessionManager = clients.sessionManager,
            decode = { it.body() },
            request = {
                clients.authenticated.patch("/me") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            },
        )
}
