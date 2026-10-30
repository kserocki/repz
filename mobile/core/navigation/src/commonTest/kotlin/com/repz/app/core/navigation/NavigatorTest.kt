package com.repz.app.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigatorTest {
    private val topLevelRoots: Map<TopLevelDestination, NavKey> =
        mapOf(
            TopLevelDestination.Train to FixtureTrainRoot,
            TopLevelDestination.History to FixtureHistoryRoot,
            TopLevelDestination.Exercises to FixtureExercisesRoot,
            TopLevelDestination.Profile to FixtureProfileRoot,
        )
    private val authRoot: NavKey = FixtureAuthWelcomeRoute

    private fun navigator(
        isAuthenticated: Boolean,
        restoredState: NavigatorState? = null,
    ) = Navigator(
        topLevelRoots = topLevelRoots,
        authRoot = authRoot,
        isAuthenticated = isAuthenticated,
        restoredState = restoredState,
    )

    @Test
    fun given_signedInNavigator_whenCreated_thenVisibleBackStackIsTrainRoot() {
        val navigator = navigator(isAuthenticated = true)

        assertEquals(TopLevelDestination.Train, navigator.selectedDestination)
        assertEquals(listOf(FixtureTrainRoot), navigator.visibleBackStack)
    }

    @Test
    fun given_signedOutNavigator_whenCreated_thenVisibleBackStackIsAuthRoot() {
        val navigator = navigator(isAuthenticated = false)

        assertFalse(navigator.isAuthenticated)
        assertEquals(listOf(FixtureAuthWelcomeRoute), navigator.visibleBackStack)
    }

    @Test
    fun given_signedInNavigator_whenNavigate_thenPushesOnlyOnSelectedTabStack() {
        val navigator = navigator(isAuthenticated = true)

        navigator.navigate(FixtureWorkoutDetailsRoute(workoutId = "w1", setIndex = null))

        assertEquals(
            listOf(FixtureTrainRoot, FixtureWorkoutDetailsRoute(workoutId = "w1", setIndex = null)),
            navigator.visibleBackStack,
        )
        val savedState = navigator.saveState()
        assertEquals(listOf(FixtureHistoryRoot), savedState.topLevelBackStacks.getValue(TopLevelDestination.History))
        assertEquals(listOf(FixtureExercisesRoot), savedState.topLevelBackStacks.getValue(TopLevelDestination.Exercises))
        assertEquals(listOf(FixtureProfileRoot), savedState.topLevelBackStacks.getValue(TopLevelDestination.Profile))
    }

    @Test
    fun given_entriesPushedOnTwoTabs_whenSwitchingTabs_thenEachStackIsPreserved() {
        val navigator = navigator(isAuthenticated = true)

        navigator.navigate(FixtureWorkoutDetailsRoute(workoutId = "w1", setIndex = null))
        navigator.selectTopLevel(TopLevelDestination.History)
        navigator.navigate(FixtureHistoryEntryRoute(workoutId = "w1"))
        navigator.selectTopLevel(TopLevelDestination.Train)

        assertEquals(
            listOf(FixtureTrainRoot, FixtureWorkoutDetailsRoute(workoutId = "w1", setIndex = null)),
            navigator.visibleBackStack,
        )
        navigator.selectTopLevel(TopLevelDestination.History)
        assertEquals(
            listOf(
                FixtureTrainRoot,
                FixtureWorkoutDetailsRoute(workoutId = "w1", setIndex = null),
                FixtureHistoryRoot,
                FixtureHistoryEntryRoute(workoutId = "w1"),
            ),
            navigator.visibleBackStack,
        )
    }

    @Test
    fun given_tabWithPushedEntry_whenReselectingSameTab_thenPopsToRoot() {
        val navigator = navigator(isAuthenticated = true)
        navigator.selectTopLevel(TopLevelDestination.History)
        navigator.navigate(FixtureHistoryEntryRoute(workoutId = "w1"))

        navigator.selectTopLevel(TopLevelDestination.History)

        assertEquals(listOf(FixtureHistoryRoot), navigator.saveState().topLevelBackStacks.getValue(TopLevelDestination.History))
    }

    @Test
    fun given_selectedTabWithMultipleEntries_whenGoBack_thenPopsTopEntryAndStaysOnTab() {
        val navigator = navigator(isAuthenticated = true)
        navigator.selectTopLevel(TopLevelDestination.History)
        navigator.navigate(FixtureHistoryEntryRoute(workoutId = "w1"))

        val handled = navigator.goBack()

        assertTrue(handled)
        assertEquals(TopLevelDestination.History, navigator.selectedDestination)
        assertEquals(listOf(FixtureHistoryRoot), navigator.saveState().topLevelBackStacks.getValue(TopLevelDestination.History))
    }

    @Test
    fun given_nonTrainTabAtRoot_whenGoBack_thenSelectsTrainWithItsStackIntact() {
        val navigator = navigator(isAuthenticated = true)
        navigator.navigate(FixtureWorkoutDetailsRoute(workoutId = "w1", setIndex = null))
        navigator.selectTopLevel(TopLevelDestination.Profile)

        val handled = navigator.goBack()

        assertTrue(handled)
        assertEquals(TopLevelDestination.Train, navigator.selectedDestination)
        assertEquals(
            listOf(FixtureTrainRoot, FixtureWorkoutDetailsRoute(workoutId = "w1", setIndex = null)),
            navigator.visibleBackStack,
        )
        assertEquals(listOf(FixtureProfileRoot), navigator.saveState().topLevelBackStacks.getValue(TopLevelDestination.Profile))
    }

    @Test
    fun given_trainTabAtRoot_whenGoBack_thenReturnsFalse() {
        val navigator = navigator(isAuthenticated = true)

        val handled = navigator.goBack()

        assertFalse(handled)
        assertEquals(listOf(FixtureTrainRoot), navigator.visibleBackStack)
    }

    @Test
    fun given_nonTrainTabSelected_whenVisibleBackStackRead_thenIncludesTrainStackBeneathSelectedStack() {
        val navigator = navigator(isAuthenticated = true)
        navigator.navigate(FixtureWorkoutDetailsRoute(workoutId = "w1", setIndex = null))
        navigator.selectTopLevel(TopLevelDestination.Exercises)

        assertEquals(
            listOf(FixtureTrainRoot, FixtureWorkoutDetailsRoute(workoutId = "w1", setIndex = null), FixtureExercisesRoot),
            navigator.visibleBackStack,
        )
    }

    @Test
    fun given_signedOutNavigatorWithDirtyTabs_whenOnSignedIn_thenAllTabsResetToRootAndTrainSelected() {
        val dirtyState =
            NavigatorState(
                isAuthenticated = false,
                selectedDestination = TopLevelDestination.Profile,
                topLevelBackStacks =
                    mapOf(
                        TopLevelDestination.Train to listOf(FixtureTrainRoot, FixtureWorkoutDetailsRoute("w1", null)),
                        TopLevelDestination.History to listOf(FixtureHistoryRoot),
                        TopLevelDestination.Exercises to listOf(FixtureExercisesRoot),
                        TopLevelDestination.Profile to listOf(FixtureProfileRoot),
                    ),
                authBackStack = listOf(FixtureAuthWelcomeRoute),
            )
        val navigator = navigator(isAuthenticated = false, restoredState = dirtyState)

        navigator.onSignedIn()

        assertTrue(navigator.isAuthenticated)
        assertEquals(TopLevelDestination.Train, navigator.selectedDestination)
        val savedState = navigator.saveState()
        TopLevelDestination.entries.forEach { destination ->
            assertEquals(listOf(topLevelRoots.getValue(destination)), savedState.topLevelBackStacks.getValue(destination))
        }
    }

    @Test
    fun given_signedInNavigatorWithPushedEntries_whenOnSignedOut_thenTabsResetAndAuthStackRestarted() {
        val navigator = navigator(isAuthenticated = true)
        navigator.navigate(FixtureWorkoutDetailsRoute(workoutId = "w1", setIndex = null))
        navigator.selectTopLevel(TopLevelDestination.History)
        navigator.navigate(FixtureHistoryEntryRoute(workoutId = "w1"))

        navigator.onSignedOut()

        assertFalse(navigator.isAuthenticated)
        val savedState = navigator.saveState()
        TopLevelDestination.entries.forEach { destination ->
            assertEquals(listOf(topLevelRoots.getValue(destination)), savedState.topLevelBackStacks.getValue(destination))
        }
        assertEquals(listOf(FixtureAuthWelcomeRoute), savedState.authBackStack)
        assertEquals(listOf(FixtureAuthWelcomeRoute), navigator.visibleBackStack)
    }

    @Test
    fun given_signedInNavigator_whenNavigateWithKeyAlreadyOnTop_thenIgnored() {
        val navigator = navigator(isAuthenticated = true)
        navigator.navigate(FixtureWorkoutDetailsRoute(workoutId = "w1", setIndex = null))

        navigator.navigate(FixtureWorkoutDetailsRoute(workoutId = "w1", setIndex = null))

        assertEquals(
            listOf(FixtureTrainRoot, FixtureWorkoutDetailsRoute(workoutId = "w1", setIndex = null)),
            navigator.visibleBackStack,
        )
    }

    @Test
    fun given_signedOutNavigator_whenNavigateAndGoBack_thenAuthStackPushesAndPopsAndReturnsFalseAtRoot() {
        val navigator = navigator(isAuthenticated = false)

        navigator.navigate(FixtureAuthLoginRoute())
        assertEquals(listOf(FixtureAuthWelcomeRoute, FixtureAuthLoginRoute()), navigator.visibleBackStack)

        val firstBack = navigator.goBack()
        assertTrue(firstBack)
        assertEquals(listOf(FixtureAuthWelcomeRoute), navigator.visibleBackStack)

        val secondBack = navigator.goBack()
        assertFalse(secondBack)
        assertEquals(listOf(FixtureAuthWelcomeRoute), navigator.visibleBackStack)
    }

    @Test
    fun given_topLevelRootsMissingADestination_whenConstructingNavigator_thenThrows() {
        assertFailsWith<IllegalArgumentException> {
            Navigator(
                topLevelRoots = mapOf(TopLevelDestination.Train to FixtureTrainRoot),
                authRoot = authRoot,
                isAuthenticated = true,
            )
        }
    }

    @Test
    fun given_signedOutStateWithAuthEntries_whenRestored_thenAuthStackAndSignedOutFlagAreRestored() {
        val signedOutStateWithAuthEntries =
            NavigatorState(
                isAuthenticated = false,
                selectedDestination = TopLevelDestination.Train,
                topLevelBackStacks =
                    mapOf(
                        TopLevelDestination.Train to listOf(FixtureTrainRoot),
                        TopLevelDestination.History to listOf(FixtureHistoryRoot),
                        TopLevelDestination.Exercises to listOf(FixtureExercisesRoot),
                        TopLevelDestination.Profile to listOf(FixtureProfileRoot),
                    ),
                authBackStack = listOf(FixtureAuthWelcomeRoute, FixtureAuthLoginRoute(prefillEmail = "a@b.com")),
            )

        val navigator = navigator(isAuthenticated = true, restoredState = signedOutStateWithAuthEntries)

        assertFalse(navigator.isAuthenticated)
        assertEquals(
            listOf(FixtureAuthWelcomeRoute, FixtureAuthLoginRoute(prefillEmail = "a@b.com")),
            navigator.visibleBackStack,
        )
    }

    @Test
    fun given_navigatorWithMutatedState_whenSavedAndRestored_thenNewNavigatorMatchesSavedState() {
        val original = navigator(isAuthenticated = true)
        original.navigate(FixtureWorkoutDetailsRoute(workoutId = "w1", setIndex = 2))
        original.selectTopLevel(TopLevelDestination.Exercises)

        val savedState = original.saveState()
        val restored = navigator(isAuthenticated = false, restoredState = savedState)

        assertEquals(savedState, restored.saveState())
        assertEquals(original.visibleBackStack, restored.visibleBackStack)
    }

    @Test
    fun given_restoredStateMissingATab_whenConstructingNavigator_thenThatTabFallsBackToItsRoot() {
        val stateMissingExercises =
            NavigatorState(
                isAuthenticated = true,
                selectedDestination = TopLevelDestination.Train,
                topLevelBackStacks =
                    mapOf(
                        TopLevelDestination.Train to listOf(FixtureTrainRoot),
                        TopLevelDestination.History to listOf(FixtureHistoryRoot),
                        TopLevelDestination.Profile to listOf(FixtureProfileRoot),
                    ),
                authBackStack = listOf(FixtureAuthWelcomeRoute),
            )

        val navigator = navigator(isAuthenticated = true, restoredState = stateMissingExercises)

        assertEquals(
            listOf(FixtureExercisesRoot),
            navigator.saveState().topLevelBackStacks.getValue(TopLevelDestination.Exercises),
        )
    }

    @Test
    fun given_restoredStateWithAnEmptyTabStack_whenConstructingNavigator_thenThatTabFallsBackToItsRoot() {
        val stateWithEmptyExercisesStack =
            NavigatorState(
                isAuthenticated = true,
                selectedDestination = TopLevelDestination.Train,
                topLevelBackStacks =
                    mapOf(
                        TopLevelDestination.Train to listOf(FixtureTrainRoot),
                        TopLevelDestination.History to listOf(FixtureHistoryRoot),
                        TopLevelDestination.Exercises to emptyList(),
                        TopLevelDestination.Profile to listOf(FixtureProfileRoot),
                    ),
                authBackStack = listOf(FixtureAuthWelcomeRoute),
            )

        val navigator = navigator(isAuthenticated = true, restoredState = stateWithEmptyExercisesStack)

        assertEquals(
            listOf(FixtureExercisesRoot),
            navigator.saveState().topLevelBackStacks.getValue(TopLevelDestination.Exercises),
        )
    }
}
