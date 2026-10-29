package com.repz.app.core.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.modules.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Exercises the real platform path `rememberNavBackStack` / `rememberSerializable` use on iOS:
 * `androidx.savedstate` encode/decode with a [androidx.savedstate.serialization.SavedStateConfiguration],
 * including [NavBackStackSerializer] itself. `SavedState` is a plain Kotlin class on iOS (a
 * stubbed `Bundle` on the Android host), so this only runs on the iOS simulator.
 */
class NavigatorStateSavedStateRoundTripTest {

    private val configuration = navigationSavedStateConfiguration(fixtureCombinedNavKeySerializers)

    // Only this configuration registers FixtureUnregisteredRoute, so it can encode a SavedState
    // containing it; configuration (above) is then used to exercise the decode-side failure.
    private val configurationWithUnregisteredKeyRegistered = navigationSavedStateConfiguration(
        fixtureCombinedNavKeySerializers +
            navKeySerializers { subclass(FixtureUnregisteredRoute::class, FixtureUnregisteredRoute.serializer()) },
    )

    private val stateWithEntriesInEveryStack = NavigatorState(
        isAuthenticated = true,
        selectedDestination = TopLevelDestination.Exercises,
        topLevelBackStacks = mapOf(
            TopLevelDestination.Train to listOf(FixtureTrainRoot, FixtureWorkoutDetailsRoute(workoutId = "w1", setIndex = null)),
            TopLevelDestination.History to listOf(FixtureHistoryRoot, FixtureHistoryEntryRoute(workoutId = "w1")),
            TopLevelDestination.Exercises to listOf(FixtureExercisesRoot),
            TopLevelDestination.Profile to listOf(FixtureProfileRoot),
        ),
        authBackStack = listOf(FixtureAuthWelcomeRoute),
    )

    @Test
    fun given_navigatorStateWithEntriesInEveryStack_whenSavedStateRoundTripped_thenDecodedStateEqualsOriginal() {
        val savedState = encodeToSavedState(NavigatorState.serializer(), stateWithEntriesInEveryStack, configuration)

        val decoded = decodeFromSavedState(NavigatorState.serializer(), savedState, configuration)

        assertEquals(stateWithEntriesInEveryStack, decoded)
    }

    @Test
    fun given_navBackStackWithMixedKeys_whenSavedStateRoundTripped_thenDecodedBackStackHasSameElements() {
        val original = NavBackStack<NavKey>().apply {
            addAll(listOf(FixtureTrainRoot, FixtureWorkoutDetailsRoute(workoutId = "w1", setIndex = 4)))
        }
        val serializer = NavBackStackSerializer(PolymorphicSerializer(NavKey::class))

        val savedState = encodeToSavedState(serializer, original, configuration)
        val decoded = decodeFromSavedState(serializer, savedState, configuration)

        assertEquals(original.toList(), decoded.toList())
    }

    @Test
    fun given_navigatorStateWithAnUnregisteredKey_whenEncodedToSavedState_thenThrowsSerializationException() {
        val stateWithUnregisteredKey = stateWithEntriesInEveryStack.copy(
            topLevelBackStacks = stateWithEntriesInEveryStack.topLevelBackStacks +
                (TopLevelDestination.Train to listOf(FixtureUnregisteredRoute)),
        )

        assertFailsWith<SerializationException> {
            encodeToSavedState(NavigatorState.serializer(), stateWithUnregisteredKey, configuration)
        }
    }

    @Test
    fun given_savedStateWithAnUnregisteredDiscriminator_whenDecoded_thenThrowsSerializationException() {
        val stateWithUnregisteredKey = stateWithEntriesInEveryStack.copy(
            topLevelBackStacks = stateWithEntriesInEveryStack.topLevelBackStacks +
                (TopLevelDestination.Train to listOf(FixtureUnregisteredRoute)),
        )
        val savedState = encodeToSavedState(
            NavigatorState.serializer(),
            stateWithUnregisteredKey,
            configurationWithUnregisteredKeyRegistered,
        )

        assertFailsWith<SerializationException> {
            decodeFromSavedState(NavigatorState.serializer(), savedState, configuration)
        }
    }
}
