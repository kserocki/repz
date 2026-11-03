package com.repz.app.core.network.serialization

import com.repz.app.core.network.dto.SyncTypeDto
import com.repz.app.core.network.dto.exercise.CustomExercisePayloadDto
import com.repz.app.core.network.dto.sync.PushChangeDto
import com.repz.app.core.network.dto.sync.TemplatePayloadDto
import com.repz.app.core.network.dto.sync.WorkoutPayloadDto
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.uuid.Uuid

/**
 * Writes/reads `{type, id, deleted, payload}`. A `Delete` always writes `payload: null`; an upsert's
 * `type` is implied by its payload variant, not stored separately on the sealed class.
 */
internal object PushChangeDtoSerializer : KSerializer<PushChangeDto> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PushChangeDto")

    override fun serialize(
        encoder: Encoder,
        value: PushChangeDto,
    ) {
        val jsonEncoder = requireJsonEncoder(encoder, "PushChangeDto")
        val json = jsonEncoder.json

        val jsonObject =
            buildJsonObject {
                when (value) {
                    is PushChangeDto.UpsertWorkout -> {
                        put("type", json.encodeToJsonElement(SyncTypeDto.serializer(), SyncTypeDto.WORKOUT))
                        put("id", json.encodeToJsonElement(Uuid.serializer(), value.id))
                        put("deleted", false)
                        put("payload", json.encodeToJsonElement(WorkoutPayloadDto.serializer(), value.payload))
                    }

                    is PushChangeDto.UpsertTemplate -> {
                        put("type", json.encodeToJsonElement(SyncTypeDto.serializer(), SyncTypeDto.TEMPLATE))
                        put("id", json.encodeToJsonElement(Uuid.serializer(), value.id))
                        put("deleted", false)
                        put("payload", json.encodeToJsonElement(TemplatePayloadDto.serializer(), value.payload))
                    }

                    is PushChangeDto.UpsertCustomExercise -> {
                        put("type", json.encodeToJsonElement(SyncTypeDto.serializer(), SyncTypeDto.EXERCISE))
                        put("id", json.encodeToJsonElement(Uuid.serializer(), value.id))
                        put("deleted", false)
                        put("payload", json.encodeToJsonElement(CustomExercisePayloadDto.serializer(), value.payload))
                    }

                    is PushChangeDto.Delete -> {
                        put("type", json.encodeToJsonElement(SyncTypeDto.serializer(), value.type))
                        put("id", json.encodeToJsonElement(Uuid.serializer(), value.id))
                        put("deleted", true)
                        put("payload", JsonNull)
                    }
                }
            }

        jsonEncoder.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): PushChangeDto {
        val jsonDecoder = requireJsonDecoder(decoder, "PushChangeDto")
        val json = jsonDecoder.json
        val jsonObject = requireJsonObject(jsonDecoder.decodeJsonElement(), "PushChangeDto")

        val type = json.decodeFromJsonElement(SyncTypeDto.serializer(), requireField(jsonObject, "type", "PushChangeDto"))
        val id = json.decodeFromJsonElement(Uuid.serializer(), requireField(jsonObject, "id", "PushChangeDto"))
        val deleted = requireField(jsonObject, "deleted", "PushChangeDto").jsonPrimitive.boolean
        val payloadElement = jsonObject["payload"] ?: JsonNull

        return if (deleted) {
            requireNullPayload(payloadElement, "deleted PushChangeDto")
            PushChangeDto.Delete(type, id)
        } else {
            decodeUpsert(json, type, id, payloadElement)
        }
    }

    private fun decodeUpsert(
        json: Json,
        type: SyncTypeDto,
        id: Uuid,
        payloadElement: JsonElement,
    ): PushChangeDto =
        when (type) {
            SyncTypeDto.WORKOUT -> {
                PushChangeDto.UpsertWorkout(id, json.decodeFromJsonElement(WorkoutPayloadDto.serializer(), payloadElement))
            }

            SyncTypeDto.TEMPLATE -> {
                PushChangeDto.UpsertTemplate(id, json.decodeFromJsonElement(TemplatePayloadDto.serializer(), payloadElement))
            }

            SyncTypeDto.EXERCISE -> {
                PushChangeDto.UpsertCustomExercise(id, json.decodeFromJsonElement(CustomExercisePayloadDto.serializer(), payloadElement))
            }
        }
}
