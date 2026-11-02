package com.repz.app.core.network.dto.sync

import kotlinx.serialization.Serializable

private const val MIN_PUSH_CHANGES = 1
private const val MAX_PUSH_CHANGES = 100

/** ADR-0020: a push envelope carries 1 to 100 changes. */
@Serializable
data class PushRequestDto(
    val changes: List<PushChangeDto>,
) {
    init {
        require(changes.size in MIN_PUSH_CHANGES..MAX_PUSH_CHANGES) {
            "changes must contain between $MIN_PUSH_CHANGES and $MAX_PUSH_CHANGES items, had ${changes.size}"
        }
    }
}
