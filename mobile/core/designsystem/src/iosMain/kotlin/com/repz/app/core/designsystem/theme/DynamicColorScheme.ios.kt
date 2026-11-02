package com.repz.app.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

// iOS has no system equivalent of Android's wallpaper-derived dynamic colour.
@Composable
internal actual fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme? = null
