package com.repz.app.core.sync.engine

import com.repz.app.core.network.dto.EquipmentDto
import com.repz.app.core.network.dto.MuscleDto
import com.repz.app.core.network.dto.SyncTypeDto
import com.repz.app.core.network.dto.exercise.CustomExercisePayloadDto
import com.repz.app.core.network.dto.sync.PushChangeDto
import com.repz.app.core.network.dto.sync.encodedSizeInBytes
import com.repz.app.core.sync.testUuid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun smallDeleteChange(index: Int): PushChangeDto = PushChangeDto.Delete(SyncTypeDto.WORKOUT, testUuid(index.toString()))

private fun largeExerciseChange(nameLength: Int): PushChangeDto =
    PushChangeDto.UpsertCustomExercise(
        testUuid("1"),
        CustomExercisePayloadDto(
            name = "a".repeat(nameLength),
            primaryMuscle = MuscleDto.CHEST,
            secondaryMuscles = emptyList(),
            equipment = EquipmentDto.BARBELL,
        ),
    )

/** The smallest [largeExerciseChange] whose encoded size is at least [targetBytes]. */
private fun changeOfAtLeast(targetBytes: Int): PushChangeDto {
    var nameLength = 0
    while (largeExerciseChange(nameLength).encodedSizeInBytes() < targetBytes) nameLength++
    return largeExerciseChange(nameLength)
}

class PushBatchingTest {
    @Test
    fun givenNoChanges_whenBatching_thenReturnsNoBatches() {
        assertEquals(emptyList(), batchPushChanges(emptyList()))
    }

    @Test
    fun given250SmallChanges_whenBatching_thenSplitsIntoThreeBatchesOfAtMost100() {
        val changes = (1..250).map { smallDeleteChange(it) }

        val batches = batchPushChanges(changes)

        assertEquals(3, batches.size)
        assertEquals(listOf(100, 100, 50), batches.map { it.size })
        assertEquals(changes, batches.flatten())
    }

    @Test
    fun givenChangesWhoseTotalSizeExceedsTheByteCap_whenBatching_thenSplitsByByteSizeBeforeReachingTheCountCap() {
        // Each change is a bit over a third of the byte cap, so two fit in a batch but a third would not.
        val change = changeOfAtLeast(MAX_PUSH_BYTES / 3 + 1)
        val changes = List(5) { change }

        val batches = batchPushChanges(changes)

        assertTrue(batches.size > 1, "expected more than one batch, got ${batches.size}")
        batches.forEach { batch ->
            val batchBytes = batch.sumOf { it.encodedSizeInBytes() }
            assertTrue(batchBytes <= MAX_PUSH_BYTES, "batch of $batchBytes bytes exceeds the $MAX_PUSH_BYTES cap")
        }
        assertEquals(changes, batches.flatten())
    }

    @Test
    fun givenASingleChangeOverTheByteCap_whenBatching_thenItStillGetsItsOwnOneItemBatch() {
        val hugeChange = largeExerciseChange(MAX_PUSH_BYTES + 1000)

        val batches = batchPushChanges(listOf(hugeChange))

        assertEquals(listOf(listOf(hugeChange)), batches)
    }

    @Test
    fun givenASingleChangeOverTheByteCapAmongOthers_whenBatching_thenItIsIsolatedIntoItsOwnBatch() {
        val small1 = smallDeleteChange(1)
        val huge = largeExerciseChange(MAX_PUSH_BYTES + 1000)
        val small2 = smallDeleteChange(2)

        val batches = batchPushChanges(listOf(small1, huge, small2))

        assertEquals(listOf(listOf(small1), listOf(huge), listOf(small2)), batches)
    }
}
