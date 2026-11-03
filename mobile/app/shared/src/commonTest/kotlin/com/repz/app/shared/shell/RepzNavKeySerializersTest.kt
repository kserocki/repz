package com.repz.app.shared.shell

import com.repz.app.core.navigation.NavigatorState
import com.repz.app.core.navigation.TopLevelDestination
import com.repz.app.feature.auth.api.LoginRoute
import com.repz.app.feature.auth.api.RegisterRoute
import com.repz.app.feature.auth.api.WelcomeRoute
import com.repz.app.feature.exercises.api.ExercisesRoute
import com.repz.app.feature.history.api.HistoryRoute
import com.repz.app.feature.settings.api.ProfileRoute
import com.repz.app.feature.workout.api.TrainRoute
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression test (§3.5): every root `NavKey` is registered in [RepzNavKeySerializers], so a
 * forgotten registration fails here instead of at runtime. `LoginRoute`/`RegisterRoute` have no
 * placeholder entry yet (REP-30 adds them), but they must already round-trip so a restored
 * back stack that reached them survives process death.
 */
class RepzNavKeySerializersTest {
    private val json = Json { serializersModule = RepzNavKeySerializers }

    private val stateWithEveryRootKey =
        NavigatorState(
            isAuthenticated = true,
            selectedDestination = TopLevelDestination.History,
            topLevelBackStacks =
                mapOf(
                    TopLevelDestination.Train to listOf(TrainRoute),
                    TopLevelDestination.History to listOf(HistoryRoute),
                    TopLevelDestination.Exercises to listOf(ExercisesRoute),
                    TopLevelDestination.Profile to listOf(ProfileRoute),
                ),
            authBackStack = listOf(WelcomeRoute, RegisterRoute, LoginRoute),
        )

    @Test
    fun givenNavigatorStateWithEveryRootKey_whenJsonRoundTripped_thenDecodedStateEqualsOriginal() {
        val encoded = json.encodeToString(stateWithEveryRootKey)

        val decoded = json.decodeFromString<NavigatorState>(encoded)

        assertEquals(stateWithEveryRootKey, decoded)
    }
}
