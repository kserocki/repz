package com.repz.app.shared.shell

/**
 * The launch hand-off's phases (ADR-0025 amendment). The system splash shows only the static bar,
 * no plates; [HoldingSplashFrame] draws the identical bar at the same position from the very first
 * Compose frame, so the hand-off is invisible, while the session restores on a true cold start (no
 * saved phase yet). [LoadingTheBar] plays MOT-01 (the plates fly in and land on the bar) only on a
 * signed-out cold start. [HoldingRestoredFrame] is the same static-bar visual, but for a
 * process-death restore: `phase` itself was saved as [Done], yet this process still has to read the
 * session again, and the OS splash shows its bar once more too; once the session resolves, this
 * phase goes straight to [FadingOut] — it never plays MOT-01. [FadingOut] is the crossfade: the
 * launch layer and the destination underneath (Welcome or Train, already composed) are both on
 * screen while the logo fades out and the destination fades in, whichever phase it arrived from.
 * [Done] is permanent for the rest of the process, so a later sign-out or sign-in never replays the
 * hand-off.
 */
internal enum class LaunchHandoffPhase {
    HoldingSplashFrame,
    LoadingTheBar,
    HoldingRestoredFrame,
    FadingOut,
    Done,
}

/**
 * Pure phase transition: no Compose, no time, no side effects. [current] plus the facts known this
 * frame decide the next phase.
 *
 * [isSignedIn] is `null` while the session is still restoring — which only ever happens once per
 * process, right after start (`AuthGate`'s state starts at `Restoring` and never reverts to it). So
 * a [current] of [Done] (saved from a previous process) seen together with a `null` [isSignedIn]
 * can only mean a process-death restore, and is routed to [HoldingRestoredFrame] rather than kept
 * as [Done], so the launch layer's static bar reappears instead of a blank background. A [Done]
 * seen with a non-null [isSignedIn] is a later in-process session change (sign-out/sign-in) and
 * stays [Done], as before. [isNavigatorSynced] is `navigator.isAuthenticated == isSignedIn`
 * (ADR-0025's brand-background guard): [HoldingSplashFrame] and [HoldingRestoredFrame] both wait for
 * it before moving on, so the content [FadingOut] reveals is always the right one, and
 * [HoldingRestoredFrame] always moves on to [FadingOut] directly, never [LoadingTheBar], so MOT-01
 * never replays. [isLogoFinished] and [isFadeFinished] are one-shot events from the launch layer's
 * UI (MOT-01 settling after its post-wordmark hold, the fade tween ending) rather than continuous
 * facts, so a phase only reacts to the one it is waiting on; [Done] otherwise ignores every input
 * and never reverts.
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

        LaunchHandoffPhase.HoldingRestoredFrame -> {
            if (isSignedIn == null || !isNavigatorSynced) {
                LaunchHandoffPhase.HoldingRestoredFrame
            } else {
                LaunchHandoffPhase.FadingOut
            }
        }

        LaunchHandoffPhase.FadingOut -> {
            if (isFadeFinished) LaunchHandoffPhase.Done else LaunchHandoffPhase.FadingOut
        }

        LaunchHandoffPhase.Done -> {
            if (isSignedIn == null) LaunchHandoffPhase.HoldingRestoredFrame else LaunchHandoffPhase.Done
        }
    }
