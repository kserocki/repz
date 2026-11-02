package com.repz.app.core.network.serialization

import com.repz.app.core.network.dto.SyncTypeDto
import com.repz.app.core.network.dto.exercise.ExercisePayloadDto
import com.repz.app.core.network.dto.sync.SyncChangeDto
import com.repz.app.core.network.dto.sync.TemplatePayloadDto
import com.repz.app.core.network.dto.sync.WorkoutPayloadDto
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
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
        val jsonEncoder = encoder as? JsonEncoder ?: throw SerializationException("SyncChangeDto can only be encoded as JSON")
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
        val jsonDecoder = decoder as? JsonDecoder ?: throw SerializationException("SyncChangeDto can only be decoded from JSON")
        val json = jsonDecoder.json
        val element = jsonDecoder.decodeJsonElement()
        val jsonObject = element as? JsonObject ?: throw SerializationException("SyncChangeDto must be a JSON object")

        val typeElement = jsonObject["type"] ?: throw SerializationException("SyncChangeDto is missing \"type\"")
        val idElement = jsonObject["id"] ?: throw SerializationException("SyncChangeDto is missing \"id\"")
        val versionElement = jsonObject["version"] ?: throw SerializationException("SyncChangeDto is missing \"version\"")
        val deletedElement = jsonObject["deleted"] ?: throw SerializationException("SyncChangeDto is missing \"deleted\"")
        val updatedAtElement = jsonObject["updatedAt"] ?: throw SerializationException("SyncChangeDto is missing \"updatedAt\"")

        val type = json.decodeFromJsonElement(SyncTypeDto.serializer(), typeElement)
        val id = json.decodeFromJsonElement(Uuid.serializer(), idElement)
        val version = versionElement.jsonPrimitive.long
        val deleted = deletedElement.jsonPrimitive.boolean
        val updatedAt = json.decodeFromJsonElement(WireInstantSerializer, updatedAtElement)
        val payloadElement = jsonObject["payload"] ?: JsonNull

        return if (deleted) {
            if (payloadElement != JsonNull) throw SerializationException("A deleted SyncChangeDto must have a null payload")
            SyncChangeDto.Tombstone(type, id, version, updatedAt)
        } else {
            if (payloadElement == JsonNull) throw SerializationException("A non-deleted SyncChangeDto must have a payload")
            when (type) {
                SyncTypeDto.WORKOUT ->
                    SyncChangeDto.Workout(id, version, updatedAt, json.decodeFromJsonElement(WorkoutPayloadDto.serializer(), payloadElement))

                SyncTypeDto.TEMPLATE ->
                    SyncChangeDto.Template(id, version, updatedAt, json.decodeFromJsonElement(TemplatePayloadDto.serializer(), payloadElement))

                SyncTypeDto.EXERCISE ->
                    SyncChangeDto.Exercise(id, version, updatedAt, json.decodeFromJsonElement(ExercisePayloadDto.serializer(), payloadElement))
            }
        }
    }
}
