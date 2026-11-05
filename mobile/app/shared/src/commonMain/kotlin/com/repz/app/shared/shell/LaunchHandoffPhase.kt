package com.repz.app.shared.shell

/**
 * The launch hand-off's phases (ADR-0025 amendment). The system splash shows only the static bar,
 * no plates; [HoldingSplashFrame] draws the identical bar at the same position from the very first
 * Compose frame, so the hand-off is invisible, while the session restores. [LoadingTheBar] plays
 * MOT-01 (the plates fly in and land on the bar) only on a signed-out cold start. [FadingOut] is the
 * crossfade: the launch layer and the destination underneath (Welcome or Train, already composed)
 * are both on screen while the logo fades out and the destination fades in, whether it ran MOT-01 or
 * crossfaded straight from [HoldingSplashFrame] on a signed-in cold start. [Done] is permanent for
 * the rest of the process, so a later sign-out or sign-in never replays the hand-off.
 */
internal enum class LaunchHandoffPhase {
    HoldingSplashFrame,
    LoadingTheBar,
    FadingOut,
    Done,
}

/**
 * Pure phase transition: no Compose, no time, no side effects. [current] plus the facts known this
 * frame decide the next phase.
 *
 * [isSignedIn] is `null` while the session is still restoring. [isNavigatorSynced] is
 * `navigator.isAuthenticated == isSignedIn` (ADR-0025's brand-background guard): the hand-off only
 * leaves [HoldingSplashFrame] once the navigator has caught up to the real session, so the content
 * revealed by [FadingOut] is always the right one. [isLogoFinished] and [isFadeFinished] are
 * one-shot events from the launch layer's UI (MOT-01 settling after its post-wordmark hold, the
 * fade tween ending) rather than continuous facts, so a phase only reacts to the one it is waiting
 * on; [Done] ignores every input and never reverts.
 */
internal fun nextLaunchHandoffPhase(
    current: LaunchHandoffPhase,
    isSignedIn: Boolean?,
    isNavigatorSynced: Boolean,
    isLogoFinished: Boolean,
    isFadeFinished: Boolean,
): LaunchHandoffPhase =
    when (current) {
        LaunchHandoffPhase.HoldingSplashFrame -> {
            when {
                isSignedIn == null || !isNavigatorSynced -> LaunchHandoffPhase.HoldingSplashFrame
                isSignedIn -> LaunchHandoffPhase.FadingOut
                else -> LaunchHandoffPhase.LoadingTheBar
            }
        }

        LaunchHandoffPhase.LoadingTheBar -> {
            if (isLogoFinished) LaunchHandoffPhase.FadingOut else LaunchHandoffPhase.LoadingTheBar
        }

        LaunchHandoffPhase.FadingOut -> {
            if (isFadeFinished) LaunchHandoffPhase.Done else LaunchHandoffPhase.FadingOut
        }

        LaunchHandoffPhase.Done -> {
            LaunchHandoffPhase.Done
        }
    }
