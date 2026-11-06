package com.repz.app.core.sync.engine

import com.repz.app.core.network.dto.sync.PushChangeDto
import com.repz.app.core.network.dto.sync.encodedSizeInBytes

/** ADR-0020's request cap, per [PushRequestDto][com.repz.app.core.network.dto.sync.PushRequestDto]. */
internal const val MAX_PUSH_CHANGES = 100

/** A margin under the server's 1 MiB request cap. */
internal const val MAX_PUSH_BYTES = 900 * 1024

/**
 * Splits [changes] into requests of at most [MAX_PUSH_CHANGES] items and [MAX_PUSH_BYTES] encoded
 * bytes each, preserving order. A single change over [MAX_PUSH_BYTES] still gets its own one-item
 * batch here; [OutboxPusher]'s 413 handling is what actually deals with that.
 */
internal fun batchPushChanges(changes: List<PushChangeDto>): List<List<PushChangeDto>> {
    if (changes.isEmpty()) return emptyList()

    val batches = mutableListOf<List<PushChangeDto>>()
    var current = mutableListOf<PushChangeDto>()
    var currentBytes = 0

    for (change in changes) {
        val changeBytes = change.encodedSizeInBytes()
        val wouldOverflowCount = current.size >= MAX_PUSH_CHANGES
        val wouldOverflowBytes = current.isNotEmpty() && currentBytes + changeBytes > MAX_PUSH_BYTES
        if (wouldOverflowCount || wouldOverflowBytes) {
            batches += current
            current = mutableListOf()
            currentBytes = 0
        }
        current += change
        currentBytes += changeBytes
    }
    if (current.isNotEmpty()) batches += current
    return batches
}
