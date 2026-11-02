package com.repz.app.core.network.dto.sync

import com.repz.app.core.network.dto.SyncTypeDto
import com.repz.app.core.network.dto.exercise.ExercisePayloadDto
import com.repz.app.core.network.serialization.SyncChangeDtoSerializer
import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * One item of a `PullResponse.changes` page. The wire shape is `{type, id, version, deleted,
 * updatedAt, payload}` ([SyncChangeDtoSerializer]): the OpenAPI spec's `anyOf` for `payload` isn't
 * tied to `type`, so that pairing is enforced here by decoding `payload` with the serializer that
 * `type` selects, strictly (a payload shaped for a different `type` fails to decode).
 */
@Serializable(with = SyncChangeDtoSerializer::class)
sealed interface SyncChangeDto {
    val id: Uuid
    val version: Long
    val updatedAt: Instant

    data class Workout(
        override val id: Uuid,
        override val version: Long,
        override val updatedAt: Instant,
        val payload: WorkoutPayloadDto,
    ) : SyncChangeDto

    data class Template(
        override val id: Uuid,
        override val version: Long,
        override val updatedAt: Instant,
        val payload: TemplatePayloadDto,
    ) : SyncChangeDto

    data class Exercise(
        override val id: Uuid,
        override val version: Long,
        override val updatedAt: Instant,
        val payload: ExercisePayloadDto,
    ) : SyncChangeDto

    data class Tombstone(
        val type: SyncTypeDto,
        override val id: Uuid,
        override val version: Long,
        override val updatedAt: Instant,
    ) : SyncChangeDto
}
