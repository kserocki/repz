package com.repz.app.core.network.serialization

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

@kotlinx.serialization.Serializable
private data class DoubleWrapper(
    @kotlinx.serialization.Serializable(with = WireDoubleSerializer::class) val value: Double,
)

class WireDoubleSerializerTest {
    @Test
    fun givenAWholeNumber_whenEncoded_thenNoDecimalPointIsWritten() {
        val encoded = Json.encodeToString(DoubleWrapper.serializer(), DoubleWrapper(8.0))

        assertEquals("""{"value":8}""", encoded)
    }

    @Test
    fun givenAFractionalNumber_whenEncoded_thenItIsWrittenAsIs() {
        val encoded = Json.encodeToString(DoubleWrapper.serializer(), DoubleWrapper(8.5))

        assertEquals("""{"value":8.5}""", encoded)
    }

    @Test
    fun givenAWholeNumberLiteral_whenDecoded_thenItDecodesToADouble() {
        val decoded = Json.decodeFromString(DoubleWrapper.serializer(), """{"value":8}""")

        assertEquals(8.0, decoded.value)
    }
}
