package com.repz.app.core.network.serialization

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
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

/**
 * Hand-written decoders don't get kotlinx.serialization's built-in unknown-key rejection for free,
 * so this mirrors the standard `ignoreUnknownKeys` behaviour of a generated decoder: a no-op in
 * production (the network `Json` config tolerates an additive server field), but strict under the
 * contract test's `Json` config, so a fixture that grows an undocumented key fails to decode
 * instead of silently succeeding.
 */
internal fun requireNoUnknownKeys(
    json: Json,
    jsonObject: JsonObject,
    allowedKeys: Set<String>,
    typeName: String,
) {
    if (json.configuration.ignoreUnknownKeys) return
    val unknownKeys = jsonObject.keys - allowedKeys
    if (unknownKeys.isNotEmpty()) {
        throw SerializationException("$typeName has unknown key(s): ${unknownKeys.sorted()}")
    }
}

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
