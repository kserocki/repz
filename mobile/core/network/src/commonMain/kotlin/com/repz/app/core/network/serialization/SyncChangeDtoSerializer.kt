package com.repz.app.core.network.serialization

import com.repz.app.core.network.dto.SyncTypeDto
import com.repz.app.core.network.dto.exercise.ExercisePayloadDto
import com.repz.app.core.network.dto.sync.SyncChangeDto
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
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Writes/reads `{type, id, version, deleted, updatedAt, payload}`. `payload` is decoded with the
 * serializer that `type` selects, so a payload shaped for a different `type` (e.g. an exercise
 * payload under `type: "workout"`) fails strict object decoding instead of silently succeeding.
 */
internal object SyncChangeDtoSerializer : KSerializer<SyncChangeDto> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SyncChangeDto")

    override fun serialize(
        encoder: Encoder,
        value: SyncChangeDto,
    ) {
        val jsonEncoder = requireJsonEncoder(encoder, "SyncChangeDto")
        val json = jsonEncoder.json

        val jsonObject =
            buildJsonObject {
                put("id", json.encodeToJsonElement(Uuid.serializer(), value.id))
                put("version", value.version)
                put("updatedAt", json.encodeToJsonElement(WireInstantSerializer, value.updatedAt))
                when (value) {
                    is SyncChangeDto.Workout -> {
                        put("type", json.encodeToJsonElement(SyncTypeDto.serializer(), SyncTypeDto.WORKOUT))
                        put("deleted", false)
                        put("payload", json.encodeToJsonElement(WorkoutPayloadDto.serializer(), value.payload))
                    }

                    is SyncChangeDto.Template -> {
                        put("type", json.encodeToJsonElement(SyncTypeDto.serializer(), SyncTypeDto.TEMPLATE))
                        put("deleted", false)
                        put("payload", json.encodeToJsonElement(TemplatePayloadDto.serializer(), value.payload))
                    }

                    is SyncChangeDto.Exercise -> {
                        put("type", json.encodeToJsonElement(SyncTypeDto.serializer(), SyncTypeDto.EXERCISE))
                        put("deleted", false)
                        put("payload", json.encodeToJsonElement(ExercisePayloadDto.serializer(), value.payload))
                    }

                    is SyncChangeDto.Tombstone -> {
                        put("type", json.encodeToJsonElement(SyncTypeDto.serializer(), value.type))
                        put("deleted", true)
                        put("payload", JsonNull)
                    }
                }
            }

        jsonEncoder.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): SyncChangeDto {
        val jsonDecoder = requireJsonDecoder(decoder, "SyncChangeDto")
        val json = jsonDecoder.json
        val jsonObject = requireJsonObject(jsonDecoder.decodeJsonElement(), "SyncChangeDto")

        val type = json.decodeFromJsonElement(SyncTypeDto.serializer(), requireField(jsonObject, "type", "SyncChangeDto"))
        val id = json.decodeFromJsonElement(Uuid.serializer(), requireField(jsonObject, "id", "SyncChangeDto"))
        val version = requireField(jsonObject, "version", "SyncChangeDto").jsonPrimitive.long
        val deleted = requireField(jsonObject, "deleted", "SyncChangeDto").jsonPrimitive.boolean
        val updatedAt = json.decodeFromJsonElement(WireInstantSerializer, requireField(jsonObject, "updatedAt", "SyncChangeDto"))
        val payloadElement = jsonObject["payload"] ?: JsonNull

        return if (deleted) {
            requireNullPayload(payloadElement, "deleted SyncChangeDto")
            SyncChangeDto.Tombstone(type, id, version, updatedAt)
        } else {
            requireNonNullPayload(payloadElement, "non-deleted SyncChangeDto")
            decodePayload(json, type, id, version, updatedAt, payloadElement)
        }
    }

    private fun decodePayload(
        json: Json,
        type: SyncTypeDto,
        id: Uuid,
        version: Long,
        updatedAt: Instant,
        payloadElement: JsonElement,
    ): SyncChangeDto =
        when (type) {
            SyncTypeDto.WORKOUT -> {
                SyncChangeDto.Workout(id, version, updatedAt, json.decodeFromJsonElement(WorkoutPayloadDto.serializer(), payloadElement))
            }

            SyncTypeDto.TEMPLATE -> {
                SyncChangeDto.Template(id, version, updatedAt, json.decodeFromJsonElement(TemplatePayloadDto.serializer(), payloadElement))
            }

            SyncTypeDto.EXERCISE -> {
                SyncChangeDto.Exercise(id, version, updatedAt, json.decodeFromJsonElement(ExercisePayloadDto.serializer(), payloadElement))
            }
        }
}
