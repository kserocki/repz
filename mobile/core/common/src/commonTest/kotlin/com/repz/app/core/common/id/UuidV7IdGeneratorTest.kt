package com.repz.app.core.common.id

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val IETF_VARIANT_NIBBLES = "89ab"

class UuidV7IdGeneratorTest {
    @Test
    fun givenGenerator_whenIdGenerated_thenVersionIs7AndVariantIsIetf() {
        val generator = UuidV7IdGenerator()

        val groups = generator.newId().toHexDashString().split("-")

        assertEquals('7', groups[2][0], "the version nibble must be 7")
        assertTrue(groups[3][0] in IETF_VARIANT_NIBBLES, "the variant nibble must mark the IETF variant")
    }

    @Test
    fun givenGenerator_when1000IdsGenerated_thenAllDistinct() {
        val generator = UuidV7IdGenerator()

        val ids = List(1000) { generator.newId() }

        assertEquals(1000, ids.toSet().size)
    }

    @Test
    fun givenGenerator_whenIdsGeneratedInSequence_thenTheyAscend() {
        val generator = UuidV7IdGenerator()

        val ids = List(1000) { generator.newId() }

        ids.zipWithNext().forEach { (earlier, later) -> assertTrue(earlier < later) }
    }
}
