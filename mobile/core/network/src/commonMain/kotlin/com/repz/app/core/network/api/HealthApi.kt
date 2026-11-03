package com.repz.app.core.network.api

import com.repz.app.core.network.dto.health.HealthResponseDto
import com.repz.app.core.network.result.NetworkResult
import com.repz.app.core.network.result.apiCall
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

interface HealthApi {
    suspend fun check(): NetworkResult<HealthResponseDto>
}

internal class KtorHealthApi(
    private val publicClient: HttpClient,
) : HealthApi {
    override suspend fun check(): NetworkResult<HealthResponseDto> =
        apiCall(
            decode = { it.body() },
            request = { publicClient.get("/health") },
        )
}
