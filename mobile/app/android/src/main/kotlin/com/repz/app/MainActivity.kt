package com.repz.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.repz.app.shared.RepzRoot

/**
 * The launcher activity. `installSplashScreen()` must run before `super.onCreate()` so the
 * AndroidX compat library can capture the starting theme (`Theme.Repz.Starting`) before it swaps
 * to `postSplashScreenTheme`. Nothing is loaded here, so there is no `setKeepOnScreenCondition`:
 * the splash exits with the system's default animation on the first Compose frame.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RepzRoot()
        }
    }
}
