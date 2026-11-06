package com.repz.app.core.sync.fakes

import com.repz.app.core.network.api.SyncApi
import com.repz.app.core.network.dto.SyncTypeDto
import com.repz.app.core.network.dto.sync.AcceptedChangeDto
import com.repz.app.core.network.dto.sync.PullResponseDto
import com.repz.app.core.network.dto.sync.PushChangeDto
import com.repz.app.core.network.dto.sync.PushRequestDto
import com.repz.app.core.network.dto.sync.PushResponseDto
import com.repz.app.core.network.dto.sync.TotalEstimateDto
import com.repz.app.core.network.result.NetworkResult
import kotlinx.coroutines.CompletableDeferred

private fun PushChangeDto.toDefaultAccepted(): AcceptedChangeDto {
    val (type, deleted) =
        when (this) {
            is PushChangeDto.UpsertWorkout -> SyncTypeDto.WORKOUT to false
            is PushChangeDto.UpsertTemplate -> SyncTypeDto.TEMPLATE to false
            is PushChangeDto.UpsertCustomExercise -> SyncTypeDto.EXERCISE to false
            is PushChangeDto.Delete -> type to true
        }
    return AcceptedChangeDto(type, id, version = 1, deleted = deleted)
}

/**
 * Scripts [SyncApi] responses per call (Konsist A5 - `core:sync` is an allowed DTO-importing
 * module) and records every request body, so a test can assert both the engine's outcome and
 * exactly what it sent. [pullHandler] is keyed by the cursor the engine asks for, which lets a
 * multi-page pull be scripted as one map instead of an ordered queue. [callLog] records `"push"`/
 * `"pull"` in call order, so a test can assert push-before-pull without caring about exact bodies.
 * [pushHandler]'s default accepts every change it is sent, matching a real server's usual case and
 * letting a test that doesn't care about the exact response (e.g. one about batching) still see the
 * outbox clear normally; [enqueuePushResponse] overrides it for one call at a time.
 */
internal class FakeSyncApi : SyncApi {
    val pushRequests = mutableListOf<PushRequestDto>()
    val pullCalls = mutableListOf<Pair<Long, Int>>()
    val callLog = mutableListOf<String>()

    private val pushResponses = ArrayDeque<NetworkResult<PushResponseDto>>()
    var pushHandler: (PushRequestDto) -> NetworkResult<PushResponseDto> = { request ->
        NetworkResult.Success(PushResponseDto(request.changes.map { it.toDefaultAccepted() }, emptyList()))
    }
    var pullHandler: (cursor: Long, limit: Int) -> NetworkResult<PullResponseDto> = { _, _ -> emptyPullResponse() }

    /** [reachedPullGate] completes right before a pull suspends on [pullGate] - a test waits on it instead of assuming timing. */
    var pullGate: CompletableDeferred<Unit>? = null
    var reachedPullGate: CompletableDeferred<Unit>? = null

    /** Same idea as the pull gate, for a test that needs to catch the engine mid-push. */
    var pushGate: CompletableDeferred<Unit>? = null
    var reachedPushGate: CompletableDeferred<Unit>? = null

    /** Overrides [pushHandler] for exactly the next [push] call. */
    fun enqueuePushResponse(response: NetworkResult<PushResponseDto>) {
        pushResponses += response
    }

    override suspend fun push(request: PushRequestDto): NetworkResult<PushResponseDto> {
        pushRequests += request
        callLog += "push"
        reachedPushGate?.complete(Unit)
        pushGate?.await()
        return if (pushResponses.isNotEmpty()) pushResponses.removeFirst() else pushHandler(request)
    }

    override suspend fun pull(
        cursor: Long,
        limit: Int,
    ): NetworkResult<PullResponseDto> {
        pullCalls += cursor to limit
        callLog += "pull"
        reachedPullGate?.complete(Unit)
        pullGate?.await()
        return pullHandler(cursor, limit)
    }

    companion object {
        fun emptyPullResponse(): NetworkResult.Success<PullResponseDto> =
            NetworkResult.Success(PullResponseDto(emptyList(), 0L, hasMore = false, TotalEstimateDto(0, 0, 0, 0)))
    }
}
