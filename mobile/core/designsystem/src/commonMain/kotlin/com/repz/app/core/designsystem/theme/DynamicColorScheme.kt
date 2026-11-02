package com.repz.app.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * The platform's dynamic (wallpaper-derived) M3 colour scheme, or `null` where dynamic colour
 * isn't available: Android below API 31, and iOS (no system equivalent).
 */
@Composable
internal expect fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme?
