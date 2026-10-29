package com.repz.app.core.navigation

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NavigatorStateJsonRoundTripTest {

    private val json = Json { serializersModule = fixtureCombinedNavKeySerializers }

    // Only this Json instance registers FixtureUnregisteredRoute, so it can encode a payload
    // containing it; json (above) is then used to exercise the decode-side failure.
    private val jsonWithUnregisteredKeyRegistered = Json {
        serializersModule = fixtureCombinedNavKeySerializers +
            navKeySerializers { subclass(FixtureUnregisteredRoute::class, FixtureUnregisteredRoute.serializer()) }
    }

    private val stateWithEntriesInEveryStack = NavigatorState(
        isAuthenticated = true,
        selectedDestination = TopLevelDestination.History,
        topLevelBackStacks = mapOf(
            TopLevelDestination.Train to listOf(FixtureTrainRoot, FixtureWorkoutDetailsRoute(workoutId = "w1", setIndex = 2)),
            TopLevelDestination.History to listOf(FixtureHistoryRoot, FixtureHistoryEntryRoute(workoutId = "w1")),
            TopLevelDestination.Exercises to listOf(FixtureExercisesRoot),
            TopLevelDestination.Profile to listOf(FixtureProfileRoot),
        ),
        authBackStack = listOf(FixtureAuthWelcomeRoute, FixtureAuthLoginRoute(prefillEmail = "a@b.com")),
    )

    @Test
    fun given_navigatorStateWithEntriesInEveryStack_whenJsonRoundTripped_thenDecodedStateEqualsOriginal() {
        val encoded = json.encodeToString(stateWithEntriesInEveryStack)

        val decoded = json.decodeFromString<NavigatorState>(encoded)

        assertEquals(stateWithEntriesInEveryStack, decoded)
    }

    @Test
    fun given_navigatorStateWithAnUnregisteredKey_whenEncoded_thenThrowsSerializationException() {
        val stateWithUnregisteredKey = stateWithEntriesInEveryStack.copy(
            topLevelBackStacks = stateWithEntriesInEveryStack.topLevelBackStacks +
                (TopLevelDestination.Train to listOf(FixtureUnregisteredRoute)),
        )

        assertFailsWith<SerializationException> {
            json.encodeToString(stateWithUnregisteredKey)
        }
    }

    @Test
    fun given_payloadWithAnUnregisteredDiscriminator_whenDecoded_thenThrowsSerializationException() {
        val stateWithUnregisteredKey = stateWithEntriesInEveryStack.copy(
            topLevelBackStacks = stateWithEntriesInEveryStack.topLevelBackStacks +
                (TopLevelDestination.Train to listOf(FixtureUnregisteredRoute)),
        )
        val payload = jsonWithUnregisteredKeyRegistered.encodeToString(stateWithUnregisteredKey)

        assertFailsWith<SerializationException> {
            json.decodeFromString<NavigatorState>(payload)
        }
    }
}
