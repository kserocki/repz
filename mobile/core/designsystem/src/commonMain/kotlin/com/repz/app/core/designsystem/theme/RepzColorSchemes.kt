package com.repz.app.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.repz.app.core.designsystem.RepzBrandColors

/**
 * The brand M3 colour schemes (DESIGN_SYSTEM §3.1), Fidelity variant seeded on brandCyan.
 * `primaryContainer`/`onPrimaryContainer` are pinned to the brand cyan/ink pair instead of a
 * generated tone, so the hero button, FAB and rest pill never drift from the logo and app icon.
 *
 * Only the 36 roles DESIGN_SYSTEM documents are overridden; the "fixed" roles
 * ([ColorScheme.primaryFixed] and friends) are not part of the design and keep the M3 baseline
 * defaults, since no Repz component reads them.
 */
internal val RepzLightColorScheme: ColorScheme =
    lightColorScheme(
        primary = Color(0xFF006879),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = RepzBrandColors.Cyan,
        onPrimaryContainer = RepzBrandColors.Ink,
        inversePrimary = Color(0xFF3CD8F6),
        secondary = Color(0xFF356570),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFB7E8F5),
        onSecondaryContainer = Color(0xFF061F24),
        tertiary = Color(0xFF8D4F00),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFF79C3C),
        onTertiaryContainer = Color(0xFF2E1500),
        background = Color(0xFFF4FAFD),
        onBackground = Color(0xFF161D1E),
        surface = Color(0xFFF4FAFD),
        onSurface = Color(0xFF161D1E),
        surfaceVariant = Color(0xFFD7E5E9),
        onSurfaceVariant = Color(0xFF3C494C),
        surfaceTint = Color(0xFF006879),
        inverseSurface = Color(0xFF2B3133),
        inverseOnSurface = Color(0xFFECF2F4),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF93000A),
        outline = Color(0xFF6C797D),
        outlineVariant = Color(0xFFBBC9CD),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFFF4FAFD),
        surfaceDim = Color(0xFFD5DBDD),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFEEF5F7),
        surfaceContainer = Color(0xFFE9EFF1),
        surfaceContainerHigh = Color(0xFFE3E9EB),
        surfaceContainerHighest = Color(0xFFDDE3E6),
    )

internal val RepzDarkColorScheme: ColorScheme =
    darkColorScheme(
        primary = Color(0xFF46DEFD),
        onPrimary = Color(0xFF003640),
        primaryContainer = RepzBrandColors.Cyan,
        onPrimaryContainer = RepzBrandColors.Ink,
        inversePrimary = Color(0xFF006879),
        secondary = Color(0xFF9ECEDB),
        onSecondary = Color(0xFF003640),
        secondaryContainer = Color(0xFF1A4D58),
        onSecondaryContainer = Color(0xFFCEE7EE),
        tertiary = Color(0xFFFFC086),
        onTertiary = Color(0xFF4B2800),
        tertiaryContainer = Color(0xFFF79C3C),
        onTertiaryContainer = Color(0xFF2E1500),
        background = Color(0xFF0E1416),
        onBackground = Color(0xFFDDE3E6),
        surface = Color(0xFF0E1416),
        onSurface = Color(0xFFDDE3E6),
        surfaceVariant = Color(0xFF3C494C),
        onSurfaceVariant = Color(0xFFBBC9CD),
        surfaceTint = Color(0xFF3CD8F6),
        inverseSurface = Color(0xFFDDE3E6),
        inverseOnSurface = Color(0xFF2B3133),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFF869397),
        outlineVariant = Color(0xFF3C494C),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFF343A3C),
        surfaceDim = Color(0xFF0E1416),
        surfaceContainerLowest = Color(0xFF090F11),
        surfaceContainerLow = Color(0xFF161D1E),
        surfaceContainer = Color(0xFF1A2122),
        surfaceContainerHigh = Color(0xFF252B2D),
        surfaceContainerHighest = Color(0xFF2F3638),
    )

/** The dynamic scheme wins when present (Android 12+ and opted in); otherwise the brand scheme
 * for the current theme (DESIGN_SYSTEM §2: dynamic colour replaces only the M3 roles). */
internal fun selectColorScheme(
    darkTheme: Boolean,
    dynamicScheme: ColorScheme?,
): ColorScheme = dynamicScheme ?: if (darkTheme) RepzDarkColorScheme else RepzLightColorScheme
