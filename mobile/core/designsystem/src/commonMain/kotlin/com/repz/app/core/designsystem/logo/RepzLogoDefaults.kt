package com.repz.app.core.designsystem.logo

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.theme.RepzTheme

/** Defaults shared by [RepzLogoSymbol], [RepzLaunchLogo] and [RepzLoader]. */
object RepzLogoDefaults {
    /** The OS splash's artwork width on both platforms (ADR-0017). */
    val SplashSymbolWidth: Dp = 144.dp

    /** brand/LOGO.md §5: deep on paper by day, cyan on ink by night, following the system
     * appearance — the same colourway the OS splash itself draws. */
    @Composable
    @ReadOnlyComposable
    fun color(): Color = if (isSystemInDarkTheme()) RepzTheme.colors.brandCyan else RepzTheme.colors.brandDeep

    /** The in-app [RepzLoader]'s default colour: the `primary` role, so it follows dynamic colour
     * like any other control (overridable by the caller). */
    @Composable
    @ReadOnlyComposable
    fun loaderColor(): Color = MaterialTheme.colorScheme.primary

    /** [RepzLoader]'s pending-pair dashed outline (board 07 A5): always `outlineVariant`, not
     * overridable — it marks an empty slot, not the brand mark itself. */
    @Composable
    @ReadOnlyComposable
    fun pendingOutlineColor(): Color = MaterialTheme.colorScheme.outlineVariant
}
