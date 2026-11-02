package com.repz.app.core.network.api

import com.repz.app.core.network.client.RepzHttpClients
import com.repz.app.core.network.dto.sync.PullResponseDto
import com.repz.app.core.network.dto.sync.PushRequestDto
import com.repz.app.core.network.dto.sync.PushResponseDto
import com.repz.app.core.network.result.NetworkResult
import com.repz.app.core.network.result.apiCall
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** The default (and maximum) page size for [SyncApi.pull] (ADR-0020). */
const val MAX_PULL_LIMIT = 500

interface SyncApi {
    /** A 413 is `Api(413, problem)`; per-change rejections are inside a `Success`. */
    suspend fun push(request: PushRequestDto): NetworkResult<PushResponseDto>

    suspend fun pull(
        cursor: Long,
        limit: Int = MAX_PULL_LIMIT,
    ): NetworkResult<PullResponseDto>
}

internal class KtorSyncApi(
    private val clients: RepzHttpClients,
) : SyncApi {
    override suspend fun push(request: PushRequestDto): NetworkResult<PushResponseDto> =
        apiCall(
            sessionManager = clients.sessionManager,
            decode = { it.body() },
            request = {
                clients.authenticated.post("/sync/push") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            },
        )

    override suspend fun pull(
        cursor: Long,
        limit: Int,
    ): NetworkResult<PullResponseDto> {
        require(cursor >= 0) { "cursor must be >= 0, was $cursor" }
        require(limit in 1..MAX_PULL_LIMIT) { "limit must be in 1..$MAX_PULL_LIMIT, was $limit" }

        return apiCall(
            sessionManager = clients.sessionManager,
            decode = { it.body() },
            request = {
                clients.authenticated.get("/sync/pull") {
                    parameter("cursor", cursor)
                    parameter("limit", limit)
                }
            },
        )
    }
}
