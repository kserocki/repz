package com.repz.app.shared.shell

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.repz.app.core.navigation.rememberNavigator
import com.repz.app.feature.auth.api.WelcomeRoute
import com.repz.app.shared.shell.ui.AppShellScaffold
import com.repz.app.shared.shell.ui.BrandBackground
import org.koin.compose.koinInject

/**
 * The real app shell behind [com.repz.app.shared.RepzRoot]: a session-driven auth gate (ADR-0025)
 * that shows the auth flow signed out and the four-tab shell signed in, reconciling a restored
 * [com.repz.app.core.navigation.Navigator] with the real session before anything but the plain
 * brand background is drawn.
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

    LaunchedEffect(isSignedIn) {
        isSignedIn?.let(navigator::syncAuthentication)
    }

    if (isSignedIn == null || navigator.isAuthenticated != isSignedIn) {
        // Restoring, or the one frame before syncAuthentication catches the navigator up.
        BrandBackground(modifier = modifier)
    } else {
        AppShellScaffold(
            selectedDestination = if (navigator.isAuthenticated) navigator.selectedDestination else null,
            onDestinationClick = navigator::selectTopLevel,
            modifier = modifier,
        ) { paddingValues ->
            RepzNavDisplay(navigator = navigator, modifier = Modifier.padding(paddingValues))
        }
    }
}
