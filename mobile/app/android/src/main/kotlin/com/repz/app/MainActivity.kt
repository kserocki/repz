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

/**
 * The launcher activity. `installSplashScreen()` must run before `super.onCreate()` so the
 * AndroidX compat library can capture the starting theme (`Theme.Repz.Starting`) before it swaps
 * to `postSplashScreenTheme`. Nothing is loaded here, so there is no `setKeepOnScreenCondition`:
 * the splash exits with the system's default animation on the first Compose frame.
 *
 * In debuggable builds only, the `repz.catalog`/`repz.dynamicColor`/`repz.reducedMotion` intent
 * extras open the debug design-system catalog instead of the normal launch placeholder, and force
 * its reduced-motion state. A release build never reads these extras, so it always renders the
 * normal placeholder, following the live platform setting, regardless of what is passed.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val catalogPageKey = if (isDebuggable) intent.getStringExtra(EXTRA_CATALOG_PAGE_KEY) else null
        val catalogDynamicColor = isDebuggable && intent.getBooleanExtra(EXTRA_CATALOG_DYNAMIC_COLOR, false)
        val catalogReducedMotion =
            if (isDebuggable && intent.hasExtra(EXTRA_CATALOG_REDUCED_MOTION)) {
                intent.getBooleanExtra(EXTRA_CATALOG_REDUCED_MOTION, false)
            } else {
                null
            }

        setContent {
            RepzRoot(
                catalogPageKey = catalogPageKey,
                catalogDynamicColor = catalogDynamicColor,
                catalogReducedMotion = catalogReducedMotion,
            )
        }
    }
}
