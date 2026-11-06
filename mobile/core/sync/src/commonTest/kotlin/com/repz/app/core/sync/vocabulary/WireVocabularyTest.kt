package com.repz.app.core.sync.vocabulary

import com.repz.app.core.network.dto.EquipmentDto
import com.repz.app.core.network.dto.MuscleDto
import com.repz.app.core.network.dto.SetTypeDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WireVocabularyTest {
    @Test
    fun givenEverySetType_whenRoundTripped_thenReturnsTheSameValue() {
        SetTypeDto.entries.forEach { value ->
            assertEquals(value, WireVocabulary.setTypeFromEntityValue(WireVocabulary.toEntityValue(value)))
        }
    }

    @Test
    fun givenEveryMuscle_whenRoundTripped_thenReturnsTheSameValue() {
        MuscleDto.entries.forEach { value ->
            assertEquals(value, WireVocabulary.muscleFromEntityValue(WireVocabulary.toEntityValue(value)))
        }
    }

    @Test
    fun givenEveryEquipment_whenRoundTripped_thenReturnsTheSameValue() {
        EquipmentDto.entries.forEach { value ->
            assertEquals(value, WireVocabulary.equipmentFromEntityValue(WireVocabulary.toEntityValue(value)))
        }
    }

    @Test
    fun givenTheKnownWireStrings_whenConverted_thenMatchTheServersLowercaseSnakeCaseVocabulary() {
        assertEquals("warmup", WireVocabulary.toEntityValue(SetTypeDto.WARMUP))
        assertEquals("full_body", WireVocabulary.toEntityValue(MuscleDto.FULL_BODY))
        assertEquals("bodyweight", WireVocabulary.toEntityValue(EquipmentDto.BODYWEIGHT))
    }

    @Test
    fun givenAnUnknownEntityValue_whenLookedUp_thenReturnsNull() {
        assertNull(WireVocabulary.setTypeFromEntityValue("not-a-set-type"))
        assertNull(WireVocabulary.muscleFromEntityValue("not-a-muscle"))
        assertNull(WireVocabulary.equipmentFromEntityValue("not-equipment"))
    }
}
