package com.repz.app.core.network.dto.sync

import com.repz.app.core.network.dto.SyncTypeDto
import com.repz.app.core.network.dto.exercise.CustomExercisePayloadDto
import com.repz.app.core.network.serialization.PushChangeDtoSerializer
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * One item of a `PushRequest.changes` array. The wire shape is `{type, id, deleted, payload}`
 * ([PushChangeDtoSerializer]); which payload type goes with which `type`/`deleted` is guaranteed by
 * the sealed hierarchy here, not re-checked at the call site.
 */
@Serializable(with = PushChangeDtoSerializer::class)
sealed interface PushChangeDto {
    val id: Uuid

    data class UpsertWorkout(
        override val id: Uuid,
        val payload: WorkoutPayloadDto,
    ) : PushChangeDto

    data class UpsertTemplate(
        override val id: Uuid,
        val payload: TemplatePayloadDto,
    ) : PushChangeDto

    data class UpsertCustomExercise(
        override val id: Uuid,
        val payload: CustomExercisePayloadDto,
    ) : PushChangeDto

    data class Delete(
        val type: SyncTypeDto,
        override val id: Uuid,
    ) : PushChangeDto
}
