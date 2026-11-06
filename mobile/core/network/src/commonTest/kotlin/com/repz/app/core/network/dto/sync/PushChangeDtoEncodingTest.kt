package com.repz.app.core.network.dto.sync

import com.repz.app.core.network.client.NetworkJson
import com.repz.app.core.network.dto.EquipmentDto
import com.repz.app.core.network.dto.MuscleDto
import com.repz.app.core.network.dto.SyncTypeDto
import com.repz.app.core.network.dto.exercise.CustomExercisePayloadDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

private val ID = Uuid.parse("00000000-0000-7000-8000-000000000001")

class PushChangeDtoEncodingTest {
    @Test
    fun givenADelete_whenMeasured_thenMatchesTheWireEncodingsByteLength() {
        val change = PushChangeDto.Delete(SyncTypeDto.WORKOUT, ID)

        val expectedBytes = NetworkJson.encodeToString(PushChangeDto.serializer(), change).encodeToByteArray().size

        assertEquals(expectedBytes, change.encodedSizeInBytes())
    }

    @Test
    fun givenALongerPayload_whenMeasured_thenTheSizeGrowsWithIt() {
        val short =
            PushChangeDto.UpsertCustomExercise(
                ID,
                CustomExercisePayloadDto("a", MuscleDto.CHEST, emptyList(), EquipmentDto.BARBELL),
            )
        val long =
            PushChangeDto.UpsertCustomExercise(
                ID,
                CustomExercisePayloadDto("a".repeat(1000), MuscleDto.CHEST, emptyList(), EquipmentDto.BARBELL),
            )

        assertEquals(true, long.encodedSizeInBytes() > short.encodedSizeInBytes() + 900)
    }
}
