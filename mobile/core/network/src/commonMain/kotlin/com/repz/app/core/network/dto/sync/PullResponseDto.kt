package com.repz.app.core.network.dto.sync

import kotlinx.serialization.Serializable

@Serializable
data class TotalEstimateDto(
    val total: Int,
    val workout: Int,
    val template: Int,
    val exercise: Int,
)

@Serializable
data class PullResponseDto(
    val changes: List<SyncChangeDto>,
    val nextCursor: Long,
    val hasMore: Boolean,
    val totalEstimate: TotalEstimateDto,
)
