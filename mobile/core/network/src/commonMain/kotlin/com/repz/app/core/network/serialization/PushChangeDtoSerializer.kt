package com.repz.app.core.network.serialization

import com.repz.app.core.network.dto.SyncTypeDto
import com.repz.app.core.network.dto.exercise.CustomExercisePayloadDto
import com.repz.app.core.network.dto.sync.PushChangeDto
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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.boolean
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
        val jsonEncoder = encoder as? JsonEncoder ?: throw SerializationException("PushChangeDto can only be encoded as JSON")
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
        val jsonDecoder = decoder as? JsonDecoder ?: throw SerializationException("PushChangeDto can only be decoded from JSON")
        val json = jsonDecoder.json
        val element = jsonDecoder.decodeJsonElement()
        val jsonObject = element as? JsonObject ?: throw SerializationException("PushChangeDto must be a JSON object")

        val typeElement = jsonObject["type"] ?: throw SerializationException("PushChangeDto is missing \"type\"")
        val idElement = jsonObject["id"] ?: throw SerializationException("PushChangeDto is missing \"id\"")
        val deletedElement = jsonObject["deleted"] ?: throw SerializationException("PushChangeDto is missing \"deleted\"")

        val type = json.decodeFromJsonElement(SyncTypeDto.serializer(), typeElement)
        val id = json.decodeFromJsonElement(Uuid.serializer(), idElement)
        val deleted = deletedElement.jsonPrimitive.boolean
        val payloadElement = jsonObject["payload"] ?: JsonNull

        return if (deleted) {
            if (payloadElement != JsonNull) throw SerializationException("A deleted PushChangeDto must have a null payload")
            PushChangeDto.Delete(type, id)
        } else {
            when (type) {
                SyncTypeDto.WORKOUT -> PushChangeDto.UpsertWorkout(id, json.decodeFromJsonElement(WorkoutPayloadDto.serializer(), payloadElement))
                SyncTypeDto.TEMPLATE -> PushChangeDto.UpsertTemplate(id, json.decodeFromJsonElement(TemplatePayloadDto.serializer(), payloadElement))
                SyncTypeDto.EXERCISE ->
                    PushChangeDto.UpsertCustomExercise(id, json.decodeFromJsonElement(CustomExercisePayloadDto.serializer(), payloadElement))
            }
        }
    }
}
