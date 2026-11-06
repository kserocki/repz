package com.repz.app.shared.shell

import com.repz.app.feature.auth.api.WelcomeRoute
import com.repz.app.feature.workout.api.ExercisePickerRoute
import com.repz.app.feature.workout.api.LiveWorkoutRoute
import com.repz.app.feature.workout.api.TrainRoute
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The predicate [RepzAppShell]'s `SignedInAppShellScaffold` feeds `AppShellScaffold.isNavigationBarVisible` with (T1). */
class FullScreenNavKeysTest {
    @Test
    fun givenTheLiveWorkoutRoute_whenChecked_thenItIsFullScreen() {
        assertTrue(LiveWorkoutRoute in FullScreenNavKeys)
    }

    @Test
    fun givenTheExercisePickerRoute_whenChecked_thenItIsFullScreen() {
        assertTrue(ExercisePickerRoute in FullScreenNavKeys)
    }

    @Test
    fun givenAnOrdinaryRoute_whenChecked_thenItIsNotFullScreen() {
        assertFalse(TrainRoute in FullScreenNavKeys)
        assertFalse(WelcomeRoute in FullScreenNavKeys)
    }
}
