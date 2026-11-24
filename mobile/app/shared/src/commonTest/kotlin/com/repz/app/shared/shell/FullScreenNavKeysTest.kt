package com.repz.app.shared.shell

import com.repz.app.feature.auth.api.WelcomeRoute
import com.repz.app.feature.workout.api.ExercisePickerRoute
import com.repz.app.feature.workout.api.LiveWorkoutRoute
import com.repz.app.feature.workout.api.TrainRoute
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The predicate [RepzAppShell]'s `SignedInAppShellScaffold` feeds `AppShellScaffold.isNavigationBarVisible` with. */
class FullScreenNavKeysTest {
    @Test
    fun givenTheLiveWorkoutRoute_whenChecked_thenItIsFullScreen() {
        assertTrue(LiveWorkoutRoute.isFullScreen())
    }

    @Test
    fun givenTheExercisePickerRouteInAddMode_whenChecked_thenItIsFullScreen() {
        assertTrue(ExercisePickerRoute().isFullScreen())
    }

    @Test
    fun givenTheExercisePickerRouteInReplaceMode_whenChecked_thenItIsFullScreen() {
        assertTrue(ExercisePickerRoute(replacingWorkoutExerciseId = "some-id").isFullScreen())
    }

    @Test
    fun givenAnOrdinaryRouteOrNull_whenChecked_thenItIsNotFullScreen() {
        assertFalse(TrainRoute.isFullScreen())
        assertFalse(WelcomeRoute.isFullScreen())
        assertFalse(null.isFullScreen())
    }
}
