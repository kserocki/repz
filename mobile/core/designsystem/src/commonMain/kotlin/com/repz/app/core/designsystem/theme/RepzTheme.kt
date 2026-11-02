package com.repz.app.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember

/**
 * The Repz design-system theme (DESIGN_SYSTEM §2–7): brand or dynamic M3 colours, the extended
 * (fixed) colours, Archivo/Inter typography, the M3 Expressive shape scale.
 *
 * [dynamicColor] (Android 12+ only) replaces only the M3 roles read through
 * `MaterialTheme.colorScheme`; [RepzTheme.colors] never follows it (DESIGN_SYSTEM §2). No
 * `motionScheme` is passed, so `MaterialExpressiveTheme`'s own default applies until the motion
 * work wires `RepzMotion` in.
 */
@Composable
fun RepzTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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

    CompositionLocalProvider(
        LocalRepzColors provides colors,
        LocalRepzTypography provides typographies.second,
        LocalRepzElevation provides elevation,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
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
