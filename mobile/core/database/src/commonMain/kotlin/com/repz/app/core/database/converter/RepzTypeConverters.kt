package com.repz.app.core.database.converter

import androidx.room3.ColumnTypeConverter
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Ids round-trip through their canonical lowercase hyphenated text, instants through epoch
 * milliseconds (sub-millisecond precision is dropped on write), and a vocabulary value list
 * through comma-joined text (no vocabulary value ever contains a comma; an empty list is `""`).
 */
class RepzTypeConverters {
    @ColumnTypeConverter
    fun uuidToText(uuid: Uuid): String = uuid.toString()

    @ColumnTypeConverter
    fun textToUuid(text: String): Uuid = Uuid.parse(text)

    @ColumnTypeConverter
    fun instantToEpochMillis(instant: Instant): Long = instant.toEpochMilliseconds()

    @ColumnTypeConverter
    fun epochMillisToInstant(epochMillis: Long): Instant = Instant.fromEpochMilliseconds(epochMillis)

    @ColumnTypeConverter
    fun stringListToText(values: List<String>): String = values.joinToString(separator = ",")

    @ColumnTypeConverter
    fun textToStringList(text: String): List<String> = if (text.isEmpty()) emptyList() else text.split(",")
}
