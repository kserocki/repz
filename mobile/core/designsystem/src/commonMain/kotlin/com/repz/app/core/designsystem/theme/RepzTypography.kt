package com.repz.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle

/**
 * The 4 numeric styles plus [heroLabel] (DESIGN_SYSTEM §4/§9 U1), exposed through
 * `RepzTheme.typography`. The 15 baseline and 15 emphasized M3 styles are exposed through
 * `MaterialTheme.typography` instead, since they are part of the M3
 * [androidx.compose.material3.Typography] shape.
 */
@Immutable
class RepzTypography(
    val numericHero: TextStyle,
    val numericLarge: TextStyle,
    val numericMedium: TextStyle,
    val numericSmall: TextStyle,
    val heroLabel: TextStyle,
)

internal val LocalRepzTypography =
    staticCompositionLocalOf<RepzTypography> {
        error("RepzTypography not provided: wrap the content in RepzTheme.")
    }
