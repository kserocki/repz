package com.repz.app.core.network.serialization

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private val json = Json

@kotlinx.serialization.Serializable
private data class InstantWrapper(
    @kotlinx.serialization.Serializable(with = WireInstantSerializer::class) val instant: Instant,
)

class WireInstantSerializerTest {
    @Test
    fun givenAnInstantExactlyOnASecond_whenEncoded_thenItStillHasThreeFractionalDigits() {
        val instant = Instant.parse("2026-11-01T10:00:00Z")

        val encoded = json.encodeToString(InstantWrapper.serializer(), InstantWrapper(instant))

        assertEquals("""{"instant":"2026-11-01T10:00:00.000Z"}""", encoded)
    }

    @Test
    fun givenAnInstantWithWholeMilliseconds_whenEncoded_thenItHasExactlyThreeDigits() {
        val instant = Instant.parse("2026-11-01T10:00:00.500Z")

        val encoded = json.encodeToString(InstantWrapper.serializer(), InstantWrapper(instant))

        assertEquals("""{"instant":"2026-11-01T10:00:00.500Z"}""", encoded)
    }

    @Test
    fun givenAnInstantWithSubMillisecondPrecision_whenEncoded_thenItIsTruncatedNotRounded() {
        val instant = Instant.parse("2026-11-01T10:00:00.9996789Z")

        val encoded = json.encodeToString(InstantWrapper.serializer(), InstantWrapper(instant))

        assertEquals("""{"instant":"2026-11-01T10:00:00.999Z"}""", encoded)
    }

    @Test
    fun givenAPositiveOffset_whenDecoded_thenItResolvesToTheSameInstantAsItsUtcEquivalent() {
        val fromOffset = json.decodeFromString(InstantWrapper.serializer(), """{"instant":"2026-11-01T12:00:00.000+02:00"}""")
        val fromUtc = json.decodeFromString(InstantWrapper.serializer(), """{"instant":"2026-11-01T10:00:00.000Z"}""")

        assertEquals(fromUtc.instant, fromOffset.instant)
    }

    @Test
    fun givenANegativeOffset_whenDecoded_thenItResolvesToTheSameInstantAsItsUtcEquivalent() {
        val fromOffset = json.decodeFromString(InstantWrapper.serializer(), """{"instant":"2026-11-01T05:00:00.000-05:00"}""")
        val fromUtc = json.decodeFromString(InstantWrapper.serializer(), """{"instant":"2026-11-01T10:00:00.000Z"}""")

        assertEquals(fromUtc.instant, fromOffset.instant)
    }
}
