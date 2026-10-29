package com.repz.app.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * The five fixed brand colours (DESIGN_SYSTEM §3.2, LOGO.md §3). Unlike the M3 scheme roles
 * exposed later through `RepzTheme.colors`, these never change with dynamic colour: they are the
 * logo and app-icon palette, identical in light and dark.
 */
object RepzBrandColors {
    val Cyan: Color = Color(0xFF00C2E0)
    val Deep: Color = Color(0xFF00687A)
    val Ink: Color = Color(0xFF0E1416)
    val Paper: Color = Color(0xFFF5FAFB)
    val Mist: Color = Color(0xFFA6EEFF)
}
