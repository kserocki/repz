package com.repz.app.core.navigation

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NavigatorStateJsonRoundTripTest {

    private val json = Json { serializersModule = fixtureCombinedNavKeySerializers }

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
}
