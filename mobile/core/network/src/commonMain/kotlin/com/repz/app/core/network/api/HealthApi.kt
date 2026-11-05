package com.repz.app.core.network.api

import com.repz.app.core.network.client.RepzHttpClients
import com.repz.app.core.network.dto.health.HealthResponseDto
import com.repz.app.core.network.result.NetworkResult
import com.repz.app.core.network.result.apiCall
import io.ktor.client.call.body
import io.ktor.client.request.get
import org.koin.core.annotation.Single

interface HealthApi {
    suspend fun check(): NetworkResult<HealthResponseDto>
}

@Single
internal class KtorHealthApi(
    clients: RepzHttpClients,
) : HealthApi {
    private val publicClient = clients.public

    override suspend fun check(): NetworkResult<HealthResponseDto> =
        apiCall(
            decode = { it.body() },
            request = { publicClient.get("/health") },
        )
}
