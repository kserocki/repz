package com.repz.app.core.network.dto.sync

import com.repz.app.core.network.dto.SyncTypeDto
import com.repz.app.core.network.dto.problem.ItemProblemDto
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/** `version == null` means the server holds nothing for this id (e.g. a delete of an id it never accepted). */
@Serializable
data class AcceptedChangeDto(
    val type: SyncTypeDto,
    val id: Uuid,
    val version: Long?,
    val deleted: Boolean,
)

@Serializable
data class RejectedChangeDto(
    val type: SyncTypeDto,
    val id: Uuid,
    val problem: ItemProblemDto,
)

@Serializable
data class PushResponseDto(
    val accepted: List<AcceptedChangeDto>,
    val rejected: List<RejectedChangeDto>,
)
