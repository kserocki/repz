package com.repz.app.core.network.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive

/**
 * Encodes a whole-number double without a decimal point (`8`, not `8.0`), matching how the server
 * (a JS runtime, where every number is a double and `(8.0).toString()` is `"8"`) writes `rpe` on the
 * wire. Kotlin's own `Double` encoding always includes a decimal point, which would otherwise fail
 * the contract test's byte-for-byte request round trip.
 */
internal object WireDoubleSerializer : KSerializer<Double> {
    override val descriptor = PrimitiveSerialDescriptor("WireDouble", PrimitiveKind.DOUBLE)

    override fun serialize(
        encoder: Encoder,
        value: Double,
    ) {
        val jsonEncoder = encoder as? JsonEncoder
        if (jsonEncoder == null) {
            encoder.encodeDouble(value)
            return
        }
        val isWholeNumber = value.isFinite() && value == value.toLong().toDouble()
        jsonEncoder.encodeJsonElement(if (isWholeNumber) JsonPrimitive(value.toLong()) else JsonPrimitive(value))
    }

    override fun deserialize(decoder: Decoder): Double = decoder.decodeDouble()
}
