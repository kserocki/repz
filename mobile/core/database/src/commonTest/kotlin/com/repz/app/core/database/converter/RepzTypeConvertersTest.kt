package com.repz.app.core.database.converter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlin.uuid.Uuid

class RepzTypeConvertersTest {
    private val converters = RepzTypeConverters()

    @Test
    fun givenUuid_whenConvertedToTextAndBack_thenRoundTripsThroughLowercaseHyphenatedText() {
        val uuid = Uuid.parse("0190f3e0-0000-7000-8000-000000000001")

        val text = converters.uuidToText(uuid)

        assertEquals("0190f3e0-0000-7000-8000-000000000001", text)
        assertEquals(uuid, converters.textToUuid(text))
    }

    @Test
    fun givenUppercaseUuidText_whenParsedAndConvertedBack_thenTextIsLowercase() {
        val uuid = Uuid.parse("0190F3E0-0000-7000-8000-000000000001")

        val text = converters.uuidToText(uuid)

        assertEquals("0190f3e0-0000-7000-8000-000000000001", text)
    }

    @Test
    fun givenInstantWithSubMillisecondPrecision_whenConvertedToEpochMillisAndBack_thenTruncatesToMilliseconds() {
        val instant = Instant.parse("2024-01-01T00:00:00.123456789Z")

        val epochMillis = converters.instantToEpochMillis(instant)

        assertEquals(Instant.parse("2024-01-01T00:00:00.123Z"), converters.epochMillisToInstant(epochMillis))
    }

    @Test
    fun givenEmptyList_whenConvertedToTextAndBack_thenRoundTripsAsEmptyList() {
        assertEquals("", converters.stringListToText(emptyList()))
        assertEquals(emptyList(), converters.textToStringList(""))
    }

    @Test
    fun givenSingleValue_whenConvertedToTextAndBack_thenRoundTrips() {
        val values = listOf("chest")

        val text = converters.stringListToText(values)

        assertEquals(values, converters.textToStringList(text))
    }

    @Test
    fun givenSeveralValues_whenConvertedToTextAndBack_thenRoundTripsInOrder() {
        val values = listOf("chest", "triceps", "shoulders")

        val text = converters.stringListToText(values)

        assertEquals("chest,triceps,shoulders", text)
        assertEquals(values, converters.textToStringList(text))
    }
}
