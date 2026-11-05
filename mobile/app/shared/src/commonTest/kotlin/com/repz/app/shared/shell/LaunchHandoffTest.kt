package com.repz.app.shared.shell

import kotlin.test.Test
import kotlin.test.assertEquals

class LaunchHandoffTest {
    @Test
    fun givenRestoring_whenSignedInUnknown_thenHoldsSplashFrame() {
        val phase =
            nextLaunchHandoffPhase(
                current = LaunchHandoffPhase.HoldingSplashFrame,
                isSignedIn = null,
                isNavigatorSynced = false,
                isLogoFinished = false,
                isFadeFinished = false,
            )

        assertEquals(LaunchHandoffPhase.HoldingSplashFrame, phase)
    }

    @Test
    fun givenSignedInButNavigatorNotSyncedYet_whenResolved_thenHoldsSplashFrame() {
        val phase =
            nextLaunchHandoffPhase(
                current = LaunchHandoffPhase.HoldingSplashFrame,
                isSignedIn = false,
                isNavigatorSynced = false,
                isLogoFinished = false,
                isFadeFinished = false,
            )

        assertEquals(LaunchHandoffPhase.HoldingSplashFrame, phase)
    }

    @Test
    fun givenSignedOutAndSynced_whenHoldingSplashFrame_thenMovesToLoadingTheBar() {
        val phase =
            nextLaunchHandoffPhase(
                current = LaunchHandoffPhase.HoldingSplashFrame,
                isSignedIn = false,
                isNavigatorSynced = true,
                isLogoFinished = false,
                isFadeFinished = false,
            )

        assertEquals(LaunchHandoffPhase.LoadingTheBar, phase)
    }

    @Test
    fun givenLoadingTheBar_whenLogoFinished_thenMovesToFadingOut() {
        val phase =
            nextLaunchHandoffPhase(
                current = LaunchHandoffPhase.LoadingTheBar,
                isSignedIn = false,
                isNavigatorSynced = true,
                isLogoFinished = true,
                isFadeFinished = false,
            )

        assertEquals(LaunchHandoffPhase.FadingOut, phase)
    }

    @Test
    fun givenLoadingTheBar_whenLogoStillPlaying_thenStaysLoadingTheBar() {
        val phase =
            nextLaunchHandoffPhase(
                current = LaunchHandoffPhase.LoadingTheBar,
                isSignedIn = false,
                isNavigatorSynced = true,
                isLogoFinished = false,
                isFadeFinished = false,
            )

        assertEquals(LaunchHandoffPhase.LoadingTheBar, phase)
    }

    @Test
    fun givenFadingOut_whenFadeFinished_thenMovesToDone() {
        val phase =
            nextLaunchHandoffPhase(
                current = LaunchHandoffPhase.FadingOut,
                isSignedIn = false,
                isNavigatorSynced = true,
                isLogoFinished = true,
                isFadeFinished = true,
            )

        assertEquals(LaunchHandoffPhase.Done, phase)
    }

    @Test
    fun givenFadingOut_whenFadeStillPlaying_thenStaysFadingOut() {
        val phase =
            nextLaunchHandoffPhase(
                current = LaunchHandoffPhase.FadingOut,
                isSignedIn = false,
                isNavigatorSynced = true,
                isLogoFinished = true,
                isFadeFinished = false,
            )

        assertEquals(LaunchHandoffPhase.FadingOut, phase)
    }

    @Test
    fun givenSignedInAndSynced_whenHoldingSplashFrame_thenMovesStraightToFadingOut() {
        val phase =
            nextLaunchHandoffPhase(
                current = LaunchHandoffPhase.HoldingSplashFrame,
                isSignedIn = true,
                isNavigatorSynced = true,
                isLogoFinished = false,
                isFadeFinished = false,
            )

        assertEquals(LaunchHandoffPhase.FadingOut, phase)
    }

    @Test
    fun givenDone_whenSessionSignsOutLater_thenStaysDone() {
        val phase =
            nextLaunchHandoffPhase(
                current = LaunchHandoffPhase.Done,
                isSignedIn = false,
                isNavigatorSynced = false,
                isLogoFinished = false,
                isFadeFinished = false,
            )

        assertEquals(LaunchHandoffPhase.Done, phase)
    }

    @Test
    fun givenDone_whenSessionSignsInLater_thenStaysDone() {
        val phase =
            nextLaunchHandoffPhase(
                current = LaunchHandoffPhase.Done,
                isSignedIn = true,
                isNavigatorSynced = false,
                isLogoFinished = false,
                isFadeFinished = false,
            )

        assertEquals(LaunchHandoffPhase.Done, phase)
    }

    @Test
    fun givenDoneRestoredFromProcessDeath_whenSessionUnknownAgain_thenMovesToHoldingRestoredFrame() {
        val phase =
            nextLaunchHandoffPhase(
                current = LaunchHandoffPhase.Done,
                isSignedIn = null,
                isNavigatorSynced = false,
                isLogoFinished = false,
                isFadeFinished = false,
            )

        assertEquals(LaunchHandoffPhase.HoldingRestoredFrame, phase)
    }

    @Test
    fun givenHoldingRestoredFrame_whenSessionStillUnknown_thenStaysHoldingRestoredFrame() {
        val phase =
            nextLaunchHandoffPhase(
                current = LaunchHandoffPhase.HoldingRestoredFrame,
                isSignedIn = null,
                isNavigatorSynced = false,
                isLogoFinished = false,
                isFadeFinished = false,
            )

        assertEquals(LaunchHandoffPhase.HoldingRestoredFrame, phase)
    }

    @Test
    fun givenHoldingRestoredFrame_whenSignedOutButNavigatorNotSyncedYet_thenStaysHoldingRestoredFrame() {
        val phase =
            nextLaunchHandoffPhase(
                current = LaunchHandoffPhase.HoldingRestoredFrame,
                isSignedIn = false,
                isNavigatorSynced = false,
                isLogoFinished = false,
                isFadeFinished = false,
            )

        assertEquals(LaunchHandoffPhase.HoldingRestoredFrame, phase)
    }

    @Test
    fun givenHoldingRestoredFrame_whenSignedOutAndSynced_thenMovesStraightToFadingOutWithoutLoadingTheBar() {
        val phase =
            nextLaunchHandoffPhase(
                current = LaunchHandoffPhase.HoldingRestoredFrame,
                isSignedIn = false,
                isNavigatorSynced = true,
                isLogoFinished = false,
                isFadeFinished = false,
            )

        assertEquals(LaunchHandoffPhase.FadingOut, phase)
    }

    @Test
    fun givenHoldingRestoredFrame_whenSignedInAndSynced_thenMovesStraightToFadingOut() {
        val phase =
            nextLaunchHandoffPhase(
                current = LaunchHandoffPhase.HoldingRestoredFrame,
                isSignedIn = true,
                isNavigatorSynced = true,
                isLogoFinished = false,
                isFadeFinished = false,
            )

        assertEquals(LaunchHandoffPhase.FadingOut, phase)
    }

    @Test
    fun givenHoldingRestoredFrame_whenFadingOutFinishes_thenMovesToDone() {
        val phase =
            nextLaunchHandoffPhase(
                current = LaunchHandoffPhase.FadingOut,
                isSignedIn = false,
                isNavigatorSynced = true,
                isLogoFinished = false,
                isFadeFinished = true,
            )

        assertEquals(LaunchHandoffPhase.Done, phase)
    }
}
