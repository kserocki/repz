package com.repz.app.core.network.contract

import com.repz.app.core.network.client.StrictContractJson
import com.repz.app.core.network.dto.health.HealthResponseDto
import com.repz.app.core.network.dto.sync.PullResponseDto
import com.repz.app.core.network.dto.sync.PushResponseDto
import com.repz.app.core.network.dto.sync.SyncChangeDto
import com.repz.app.core.network.dto.user.UserDto
import com.repz.app.core.network.serialization.SyncChangeDtoSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Every file under `contract/fixtures` (reached via the generated [generatedContractFixtures],
 * §3.10) decodes with [StrictContractJson] on this platform, coverage holds both ways between
 * fixtures and [CONTRACT_CATALOG], every request fixture re-encodes to the exact same wire JSON,
 * a few fixtures are checked semantically, and a handful of hand-written negative cases fail to
 * decode.
 */
class ContractFixturesTest {
    @Test
    fun givenEveryContractFixture_whenDecodedWithStrictContractJson_thenItSucceeds() {
        generatedContractFixtures.forEach { fixture ->
            val serializer = CONTRACT_CATALOG[fixture.operationKey] ?: error("No catalog entry for fixture ${fixture.path}")
            try {
                StrictContractJson.decodeFromString(serializer, fixture.json)
            } catch (e: SerializationException) {
                throw AssertionError("Fixture ${fixture.path} failed to decode: ${e.message}", e)
            }
        }
    }

    @Test
    fun givenGeneratedFixtures_whenCheckingCatalogCoverage_thenEveryFixtureHasACatalogEntry() {
        val uncovered = generatedContractFixtures.filter { it.operationKey !in CONTRACT_CATALOG }

        assertTrue(uncovered.isEmpty(), "Fixtures with no catalog entry (unknown operation key): ${uncovered.map { it.path }}")
    }

    @Test
    fun givenTheCatalog_whenCheckingFixtureCoverage_thenEveryEntryHasAtLeastOneFixture() {
        val coveredKeys = generatedContractFixtures.map { it.operationKey }.toSet()
        val unusedKeys = CONTRACT_CATALOG.keys.filter { it !in coveredKeys }

        assertTrue(unusedKeys.isEmpty(), "Catalog entries with no matching fixture: $unusedKeys")
    }

    @Test
    fun givenEveryRequestFixture_whenReencoded_thenItMatchesTheRawWireJsonExactly() {
        generatedContractFixtures.filter { "/request/" in it.path }.forEach { fixture ->
            val serializer = CONTRACT_CATALOG.getValue(fixture.operationKey)
            val reencoded = decodeAndReencode(serializer, fixture.json)
            val original = StrictContractJson.parseToJsonElement(fixture.json)

            assertEquals(original, reencoded, "Fixture ${fixture.path} did not re-encode to the same JSON")
        }
    }

    @Test
    fun givenPageWithEveryType_whenDecoded_thenEntriesDecodeInTheFixturesOrder() {
        val fixture = generatedContractFixtures.single { it.path == "syncPull/200/page-with-every-type.json" }

        val decoded = StrictContractJson.decodeFromString(PullResponseDto.serializer(), fixture.json)

        assertEquals(
            listOf(
                SyncChangeDto.Exercise::class,
                SyncChangeDto.Exercise::class,
                SyncChangeDto.Template::class,
                SyncChangeDto.Tombstone::class,
                SyncChangeDto.Workout::class,
            ),
            decoded.changes.map { it::class },
        )
        assertEquals(false, (decoded.changes[0] as SyncChangeDto.Exercise).payload.isCustom)
        assertEquals(true, (decoded.changes[1] as SyncChangeDto.Exercise).payload.isCustom)
    }

    @Test
    fun givenAcceptedDeleteOfAnUnknownId_whenDecoded_thenVersionIsNull() {
        val fixture = generatedContractFixtures.single { it.path == "syncPush/200/accepted-delete-unknown-id.json" }

        val decoded = StrictContractJson.decodeFromString(PushResponseDto.serializer(), fixture.json)

        assertNull(decoded.accepted.single().version)
    }

    @Test
    fun givenARejectedValidationError_whenDecoded_thenTheFieldErrorPathIsKept() {
        val fixture = generatedContractFixtures.single { it.path == "syncPush/200/rejected-validation-error.json" }

        val decoded = StrictContractJson.decodeFromString(PushResponseDto.serializer(), fixture.json)

        assertEquals("payload.name", decoded.rejected.single().problem.errors?.single()?.path)
    }

    @Test
    fun givenAWorkoutTypeWithAnExercisePayload_whenDecoded_thenItFailsToDecode() {
        val json =
            """
            {"type":"workout","id":"b6e6b6a0-0000-4a22-8a33-000000000001","version":1,"deleted":false,
            "updatedAt":"2026-11-01T10:00:00.000Z","payload":{"name":"x","primaryMuscle":"chest",
            "secondaryMuscles":[],"equipment":"barbell","isCustom":true,"imageUrl":null,"license":null,
            "author":null,"sourceUrl":null,"translations":[]}}
            """.trimIndent()

        assertFailsWith<SerializationException> { StrictContractJson.decodeFromString(SyncChangeDtoSerializer, json) }
    }

    @Test
    fun givenDeletedFalseWithANullPayload_whenDecoded_thenItFailsToDecode() {
        val json =
            """{"type":"workout","id":"b6e6b6a0-0000-4a22-8a33-000000000001","version":1,"deleted":false,
            "updatedAt":"2026-11-01T10:00:00.000Z","payload":null}
            """.trimIndent()

        assertFailsWith<SerializationException> { StrictContractJson.decodeFromString(SyncChangeDtoSerializer, json) }
    }

    @Test
    fun givenDeletedTrueWithAnObjectPayload_whenDecoded_thenItFailsToDecode() {
        val json =
            """{"type":"workout","id":"b6e6b6a0-0000-4a22-8a33-000000000001","version":1,"deleted":true,
            "updatedAt":"2026-11-01T10:00:00.000Z","payload":{"name":"x"}}
            """.trimIndent()

        assertFailsWith<SerializationException> { StrictContractJson.decodeFromString(SyncChangeDtoSerializer, json) }
    }

    @Test
    fun givenAnUnknownKey_whenDecodedWithStrictContractJson_thenItFailsToDecode() {
        val json = """{"status":"ok","somethingTheSpecDoesNotDocument":"nope"}"""

        assertFailsWith<SerializationException> { StrictContractJson.decodeFromString(HealthResponseDto.serializer(), json) }
    }

    @Test
    fun givenAnUnknownEnumValue_whenDecoded_thenItFailsToDecode() {
        val json = """{"id":"b6e6b6a0-0000-4a22-8a33-000000000001","email":"a@b.com","displayName":"A","unitSystem":"eur"}"""

        assertFailsWith<SerializationException> { StrictContractJson.decodeFromString(UserDto.serializer(), json) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun decodeAndReencode(
        serializer: KSerializer<*>,
        raw: String,
    ): JsonElement {
        val typedSerializer = serializer as KSerializer<Any?>
        val decoded = StrictContractJson.decodeFromString(typedSerializer, raw)
        return StrictContractJson.encodeToJsonElement(typedSerializer, decoded)
    }
}
