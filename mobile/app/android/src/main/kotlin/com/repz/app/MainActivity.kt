package com.repz.app

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.repz.app.shared.RepzRoot

private const val EXTRA_CATALOG_PAGE_KEY = "repz.catalog"
private const val EXTRA_CATALOG_DYNAMIC_COLOR = "repz.dynamicColor"
private const val EXTRA_CATALOG_REDUCED_MOTION = "repz.reducedMotion"
private const val EXTRA_DEBUG_SIGNED_IN = "repz.debugSignedIn"

/**
 * The launcher activity. `installSplashScreen()` must run before `super.onCreate()` so the
 * AndroidX compat library can capture the starting theme (`Theme.Repz.Starting`) before it swaps
 * to `postSplashScreenTheme`. Nothing is loaded here, so there is no `setKeepOnScreenCondition`.
 *
 * The exit is handed off explicitly: `setOnExitAnimationListener { it.remove() }` removes the
 * splash view the instant it fires, which the platform guarantees is only once the first Compose
 * frame underneath has already been drawn. Leaving this unset lets the system play its own default
 * icon exit animation and auto-remove the view on its own schedule — decoupled from whether Compose
 * has actually drawn anything yet, which left a blank frame between the splash fading out and the
 * app shell's launch bar (identical in shape/position) appearing underneath.
 *
 * In debuggable builds only, the `repz.catalog`/`repz.dynamicColor` intent extras open the debug
 * design-system catalog instead of the normal app shell. `repz.reducedMotion` forces reduced
 * motion on whichever path renders (the catalog or the app shell's launch hand-off), so Spotter
 * can capture MOT-01 without a device setting. The `repz.debugSignedIn` extra (ADR-0025) makes the
 * auth gate treat the session as signed in without stored tokens, so the signed-in shell can be
 * reached without logging in. A release build never reads these extras, so it always renders the
 * normal app shell, following the live platform setting, regardless of what is passed.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        splashScreen.setOnExitAnimationListener { it.remove() }

        val isDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val catalogPageKey = if (isDebuggable) intent.getStringExtra(EXTRA_CATALOG_PAGE_KEY) else null
        val catalogDynamicColor = isDebuggable && intent.getBooleanExtra(EXTRA_CATALOG_DYNAMIC_COLOR, false)
        val debugReducedMotion =
            if (isDebuggable && intent.hasExtra(EXTRA_CATALOG_REDUCED_MOTION)) {
                intent.getBooleanExtra(EXTRA_CATALOG_REDUCED_MOTION, false)
            } else {
                null
            }
        val debugSignedIn = isDebuggable && intent.getBooleanExtra(EXTRA_DEBUG_SIGNED_IN, false)

        setContent {
            RepzRoot(
                catalogPageKey = catalogPageKey,
                catalogDynamicColor = catalogDynamicColor,
                debugReducedMotion = debugReducedMotion,
                debugSignedIn = debugSignedIn,
            )
        }
    }
}
