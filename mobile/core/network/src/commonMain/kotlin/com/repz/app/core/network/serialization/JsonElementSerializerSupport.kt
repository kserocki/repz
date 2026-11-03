package com.repz.app.core.network.serialization

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/** Shared guard clauses for [PushChangeDtoSerializer] and [SyncChangeDtoSerializer], which both hand-decode a JSON object. */
internal fun requireJsonEncoder(
    encoder: Encoder,
    typeName: String,
): JsonEncoder = encoder as? JsonEncoder ?: throw SerializationException("$typeName can only be encoded as JSON")

internal fun requireJsonDecoder(
    decoder: Decoder,
    typeName: String,
): JsonDecoder = decoder as? JsonDecoder ?: throw SerializationException("$typeName can only be decoded from JSON")

internal fun requireJsonObject(
    element: JsonElement,
    typeName: String,
): JsonObject = element as? JsonObject ?: throw SerializationException("$typeName must be a JSON object")

internal fun requireField(
    jsonObject: JsonObject,
    name: String,
    typeName: String,
): JsonElement = jsonObject[name] ?: throw SerializationException("$typeName is missing \"$name\"")

internal fun requireNullPayload(
    payloadElement: JsonElement,
    context: String,
) {
    if (payloadElement != JsonNull) throw SerializationException("A $context must have a null payload")
}

internal fun requireNonNullPayload(
    payloadElement: JsonElement,
    context: String,
) {
    if (payloadElement == JsonNull) throw SerializationException("A $context must have a payload")
}
