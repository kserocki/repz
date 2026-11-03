package com.repz.app.core.network.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Instant

/**
 * Decodes any ISO-8601 offset the server might send; encodes in UTC with exactly 3 fractional
 * digits (ADR-0020 timestamps are millisecond-precision), truncating any sub-millisecond part.
 * `Instant`'s own `toString()` omits the fraction entirely when it's zero (`"...T10:00:00Z"`
 * instead of `"...T10:00:00.000Z"`), which wouldn't match the wire, so that one case is patched in.
 */
internal object WireInstantSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("WireInstant", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: Instant,
    ) {
        val truncatedToMillis = Instant.fromEpochMilliseconds(value.toEpochMilliseconds())
        val isoString = truncatedToMillis.toString()
        val wireString = if ('.' in isoString) isoString else isoString.removeSuffix("Z") + ".000Z"
        encoder.encodeString(wireString)
    }

    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}
