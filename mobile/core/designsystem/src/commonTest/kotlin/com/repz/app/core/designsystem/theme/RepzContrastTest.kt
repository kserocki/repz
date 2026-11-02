package com.repz.app.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertTrue

private const val TEXT_MIN_RATIO = 4.5f
private const val GRAPHIC_MIN_RATIO = 3f

/** WCAG 2.0 contrast of every text/graphic pair DESIGN_SYSTEM §3.3 and board 01 call out. */
class RepzContrastTest {
    @Test
    fun given_lightTheme_whenCheckingTextPairs_thenContrastIsAtLeast4Point5() {
        assertTextPairs(RepzLightColorScheme, RepzLightColors)
    }

    @Test
    fun given_darkTheme_whenCheckingTextPairs_thenContrastIsAtLeast4Point5() {
        assertTextPairs(RepzDarkColorScheme, RepzDarkColors)
    }

    @Test
    fun given_lightTheme_whenCheckingMusclePrimaryGraphic_thenContrastIsAtLeast3() {
        assertAtLeast(GRAPHIC_MIN_RATIO, RepzLightColors.musclePrimary, RepzLightColorScheme.surface, "musclePrimary/surface")
    }

    @Test
    fun given_darkTheme_whenCheckingMusclePrimaryGraphic_thenContrastIsAtLeast3() {
        assertAtLeast(GRAPHIC_MIN_RATIO, RepzDarkColors.musclePrimary, RepzDarkColorScheme.surface, "musclePrimary/surface")
    }

    private fun assertTextPairs(
        scheme: ColorScheme,
        colors: RepzColors,
    ) {
        textPairs(scheme, colors).forEach { (label, foreground, background) ->
            assertAtLeast(TEXT_MIN_RATIO, foreground, background, label)
        }
    }

    private fun assertAtLeast(
        minRatio: Float,
        foreground: Color,
        background: Color,
        label: String,
    ) {
        val ratio = contrastRatio(foreground, background)
        assertTrue(ratio >= minRatio, "$label contrast was $ratio, expected >= $minRatio")
    }
}

private data class ContrastPair(
    val label: String,
    val foreground: Color,
    val background: Color,
)

private fun textPairs(
    scheme: ColorScheme,
    colors: RepzColors,
): List<ContrastPair> = surfacePairs(scheme, colors) + containerPairs(scheme, colors) + onColorPairs(colors)

private fun surfacePairs(
    scheme: ColorScheme,
    colors: RepzColors,
) = listOf(
    ContrastPair("onSurface/surface", scheme.onSurface, scheme.surface),
    ContrastPair("onSurfaceVariant/surface", scheme.onSurfaceVariant, scheme.surface),
    ContrastPair("primary/surface", scheme.primary, scheme.surface),
    ContrastPair("onPrimary/primary", scheme.onPrimary, scheme.primary),
    ContrastPair("record/surface", colors.record, scheme.surface),
    ContrastPair("setWarmup/surface", colors.setWarmup, scheme.surface),
    ContrastPair("setNormal/surface", colors.setNormal, scheme.surface),
    ContrastPair("setDrop/surface", colors.setDrop, scheme.surface),
    ContrastPair("setFailure/surface", colors.setFailure, scheme.surface),
    ContrastPair("success/surface", colors.success, scheme.surface),
    ContrastPair("warning/surface", colors.warning, scheme.surface),
)

private fun containerPairs(
    scheme: ColorScheme,
    colors: RepzColors,
) = listOf(
    ContrastPair("onPrimaryContainer/primaryContainer", scheme.onPrimaryContainer, scheme.primaryContainer),
    ContrastPair("onSecondaryContainer/secondaryContainer", scheme.onSecondaryContainer, scheme.secondaryContainer),
    ContrastPair("onTertiaryContainer/tertiaryContainer", scheme.onTertiaryContainer, scheme.tertiaryContainer),
    ContrastPair("onErrorContainer/errorContainer", scheme.onErrorContainer, scheme.errorContainer),
    ContrastPair("onSuccessContainer/successContainer", colors.onSuccessContainer, colors.successContainer),
    ContrastPair("onWarningContainer/warningContainer", colors.onWarningContainer, colors.warningContainer),
    ContrastPair("onRecordContainer/recordContainer", colors.onRecordContainer, colors.recordContainer),
    ContrastPair("onSetWarmupContainer/setWarmupContainer", colors.onSetWarmupContainer, colors.setWarmupContainer),
    ContrastPair("onSetNormalContainer/setNormalContainer", colors.onSetNormalContainer, colors.setNormalContainer),
    ContrastPair("onSetDropContainer/setDropContainer", colors.onSetDropContainer, colors.setDropContainer),
    ContrastPair(
        "onSetFailureContainer/setFailureContainer",
        colors.onSetFailureContainer,
        colors.setFailureContainer,
    ),
)

private fun onColorPairs(colors: RepzColors) =
    listOf(
        ContrastPair("onSuccess/success", colors.onSuccess, colors.success),
        ContrastPair("onWarning/warning", colors.onWarning, colors.warning),
        ContrastPair("onRecord/record", colors.onRecord, colors.record),
        ContrastPair("onSetWarmup/setWarmup", colors.onSetWarmup, colors.setWarmup),
        ContrastPair("onSetNormal/setNormal", colors.onSetNormal, colors.setNormal),
        ContrastPair("onSetDrop/setDrop", colors.onSetDrop, colors.setDrop),
        ContrastPair("onSetFailure/setFailure", colors.onSetFailure, colors.setFailure),
    )

private fun contrastRatio(
    a: Color,
    b: Color,
): Float {
    val lighter = max(a.luminance(), b.luminance())
    val darker = min(a.luminance(), b.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}
