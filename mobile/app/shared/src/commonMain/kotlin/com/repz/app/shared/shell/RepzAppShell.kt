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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    // A fresh process starts in `HoldingSplashFrame` and never goes back once it reaches `Done`:
    // `rememberSaveable` survives a configuration change or a process-death restore, so neither
    // replays the hand-off, but a cold start (no saved state) always starts it.
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

    Box(modifier = modifier) {
        if (isNavigatorSynced) {
            SignedInAppShellScaffold(navigator)
        } else if (phase == LaunchHandoffPhase.Done) {
            // A later session change desynced the navigator for one frame (ADR-0025's guard); the
            // hand-off is already done and never replays, so this stays the plain background
            // rather than the launch layer.
            BrandBackground()
        }

        if (phase != LaunchHandoffPhase.Done) {
            LaunchHandoff(
                phase = phase,
                onLogoFinished = {
                    phase = nextLaunchHandoffPhase(phase, isSignedIn, isNavigatorSynced, isLogoFinished = true, isFadeFinished = false)
                },
                onFadeFinished = {
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
