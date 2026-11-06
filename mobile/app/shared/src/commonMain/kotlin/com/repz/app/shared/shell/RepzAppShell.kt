package com.repz.app.shared.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.repz.app.core.domain.sync.SyncScheduler
import com.repz.app.core.navigation.Navigator
import com.repz.app.core.navigation.rememberNavigator
import com.repz.app.feature.auth.api.WelcomeRoute
import com.repz.app.shared.shell.ui.AppShellScaffold
import com.repz.app.shared.shell.ui.BrandBackground
import com.repz.app.shared.shell.ui.LaunchHandoff
import org.koin.compose.koinInject

/**
 * The real app shell behind [com.repz.app.shared.RepzRoot]: a session-driven auth gate (ADR-0025)
 * that shows the auth flow signed out and the four-tab shell signed in, reconciling a restored
 * [com.repz.app.core.navigation.Navigator] with the real session before anything but the launch
 * hand-off layer is drawn (the bar matching the system splash, then MOT-01 or a straight crossfade,
 * ADR-0025 amendment).
 *
 * [debugSignedIn] makes the gate treat the session as signed in without any stored tokens, so the
 * signed-in shell can be reached by hand before a real login flow exists.
 */
@Composable
internal fun RepzAppShell(
    debugSignedIn: Boolean,
    modifier: Modifier = Modifier,
) {
    val navigator =
        rememberNavigator(
            topLevelRoots = RepzTopLevelRoots,
            authRoot = WelcomeRoute,
            configuration = RepzNavigationConfiguration,
        )
    val gate = koinInject<AuthGate>()
    val gateState by gate.state.collectAsStateWithLifecycle()
    val isSignedIn = if (debugSignedIn) true else (gateState as? AuthGateUiState.Ready)?.isSignedIn
    // ADR-0025's brand-background guard: true once the navigator has caught up to the real
    // session, so whatever the hand-off reveals underneath is always the right stack.
    val isNavigatorSynced = navigator.isAuthenticated == isSignedIn

    LaunchedEffect(isSignedIn) {
        isSignedIn?.let(navigator::syncAuthentication)
    }

    // The one shared foreground/login sync trigger (ARCHITECTURE §5.6, ADR-0027 Q7): CMP's iOS
    // lifecycle maps `willEnterForeground` to `ON_START`, so this single effect covers both
    // platforms' "app foreground" row and, since `isSignedIn` flipping to true re-keys it, "just
    // logged in" too. `SyncEngine.sync()` itself re-checks the session, so a debug-signed-in run
    // (isSignedIn true with no real session) still sends zero requests.
    val syncScheduler = koinInject<SyncScheduler>()
    LifecycleStartEffect(isSignedIn) {
        if (isSignedIn == true) {
            syncScheduler.requestSync()
        }
        onStopOrDispose {}
    }

    // A fresh process starts in `HoldingSplashFrame`. A configuration change restores `phase`
    // as-is and never replays anything, because the session is already resolved by the time one
    // can happen. A process-death restore can also land on a saved `Done` while the session is
    // unresolved again (`isSignedIn == null`, only possible right after process start):
    // `nextLaunchHandoffPhase` routes that combination to `HoldingRestoredFrame`, so the same
    // static bar the OS splash just showed reappears and then crossfades into the restored
    // destination once the session resolves — MOT-01 itself never replays.
    var phase by rememberSaveable { mutableStateOf(LaunchHandoffPhase.HoldingSplashFrame) }

    LaunchedEffect(isSignedIn, isNavigatorSynced) {
        phase =
            nextLaunchHandoffPhase(
                current = phase,
                isSignedIn = isSignedIn,
                isNavigatorSynced = isNavigatorSynced,
                isLogoFinished = false,
                isFadeFinished = false,
            )
    }

    // The launch layer is visually opaque in every phase except FadingOut (and it is never
    // composed at all once Done): whatever is revealed below must stay unreachable by
    // TalkBack/VoiceOver until the crossfade actually starts to reveal it, matching the touch
    // blocking LaunchHandoff itself does for the same window.
    val isLaunchLayerOpaque = phase != LaunchHandoffPhase.Done && phase != LaunchHandoffPhase.FadingOut
    val hiddenWhileOpaqueModifier = if (isLaunchLayerOpaque) Modifier.semantics { hideFromAccessibility() } else Modifier

    Box(modifier = modifier) {
        if (isNavigatorSynced) {
            Box(modifier = hiddenWhileOpaqueModifier) {
                SignedInAppShellScaffold(navigator)
            }
        } else if (phase == LaunchHandoffPhase.Done) {
            // A later in-process session change (sign-out/sign-in) desynced the navigator for one
            // frame (ADR-0025's guard) while the session was already known, so phase stays `Done`
            // and this is the plain background rather than the launch layer. A process-death
            // restore with the session still unknown routes through `HoldingRestoredFrame`
            // instead (see `nextLaunchHandoffPhase`), so it never reaches this branch.
            BrandBackground()
        }

        if (phase != LaunchHandoffPhase.Done) {
            LaunchHandoff(
                phase = phase,
                onLogoFinish = {
                    phase = nextLaunchHandoffPhase(phase, isSignedIn, isNavigatorSynced, isLogoFinished = true, isFadeFinished = false)
                },
                onFadeFinish = {
                    phase = nextLaunchHandoffPhase(phase, isSignedIn, isNavigatorSynced, isLogoFinished = true, isFadeFinished = true)
                },
            )
        }
    }
}

@Composable
private fun SignedInAppShellScaffold(navigator: Navigator) {
    AppShellScaffold(
        selectedDestination = if (navigator.isAuthenticated) navigator.selectedDestination else null,
        onDestinationClick = navigator::selectTopLevel,
    ) { paddingValues ->
        RepzNavDisplay(
            navigator = navigator,
            modifier = Modifier.padding(paddingValues).consumeWindowInsets(paddingValues),
        )
    }
}
