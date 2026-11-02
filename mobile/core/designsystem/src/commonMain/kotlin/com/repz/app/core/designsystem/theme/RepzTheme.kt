package com.repz.app.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import com.repz.app.core.designsystem.haptics.LocalRepzHaptics
import com.repz.app.core.designsystem.haptics.RepzHaptics
import com.repz.app.core.designsystem.haptics.rememberPlatformHapticPerformer
import com.repz.app.core.designsystem.motion.LocalReducedMotion
import com.repz.app.core.designsystem.motion.rememberSystemReducedMotion
import com.repz.app.core.designsystem.motion.repzMotionScheme

/**
 * The Repz design-system theme (DESIGN_SYSTEM §2–7): brand or dynamic M3 colours, the extended
 * (fixed) colours, Archivo/Inter typography, the M3 Expressive shape scale, `RepzMotion`'s springs
 * and tweens (MOTION.md §2) and `RepzHaptics` (MOTION.md §3).
 *
 * [dynamicColor] (Android 12+ only) replaces only the M3 roles read through
 * `MaterialTheme.colorScheme`; [RepzTheme.colors] never follows it (DESIGN_SYSTEM §2).
 *
 * [reducedMotion] `null` (the default) follows the live platform setting; `true`/`false` forces it,
 * which previews and the debug catalog's "force reduced motion" hook use. [hapticsEnabled] is the
 * F23 "Haptics" setting, wired by that feature later; it defaults to on.
 */
@Composable
fun RepzTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    reducedMotion: Boolean? = null,
    hapticsEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dynamicScheme = if (dynamicColor) platformDynamicColorScheme(darkTheme) else null
    val colorScheme = selectColorScheme(darkTheme, dynamicScheme)
    val colors = if (darkTheme) RepzDarkColors else RepzLightColors
    val fontFamilies = rememberRepzFontFamilies()
    val typographies =
        remember(fontFamilies) {
            repzMaterialTypography(fontFamilies) to repzNumericTypography(fontFamilies)
        }
    val elevation = remember(colorScheme, darkTheme) { repzElevationScheme(colorScheme, darkTheme) }
    val resolvedReducedMotion = reducedMotion ?: rememberSystemReducedMotion()
    val motionScheme = remember(resolvedReducedMotion) { repzMotionScheme(resolvedReducedMotion) }
    val hapticPerformer = rememberPlatformHapticPerformer()
    val haptics = remember(hapticPerformer, hapticsEnabled) { RepzHaptics(hapticPerformer, hapticsEnabled) }

    CompositionLocalProvider(
        LocalRepzColors provides colors,
        LocalRepzTypography provides typographies.second,
        LocalRepzElevation provides elevation,
        LocalReducedMotion provides resolvedReducedMotion,
        LocalRepzHaptics provides haptics,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = motionScheme,
            shapes = RepzMaterialShapes,
            typography = typographies.first,
            content = content,
        )
    }
}

/** `RepzTheme.colors`/`RepzTheme.typography`: the token groups DESIGN_SYSTEM doesn't expose
 * through plain `MaterialTheme`. */
object RepzTheme {
    val colors: RepzColors
        @Composable @ReadOnlyComposable
        get() = LocalRepzColors.current

    val typography: RepzTypography
        @Composable @ReadOnlyComposable
        get() = LocalRepzTypography.current
}
