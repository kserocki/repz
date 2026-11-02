package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.repz.app.core.designsystem.theme.RepzColors
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

/** Part 2 of 3 of every `RepzTheme.colors` token (DESIGN_SYSTEM §3.2): every semantic group's base
 * `x`/`onX` colours. Part 1 ([ExtendedCatalogPage1]) has the tokens with no pair (brand/graphics).
 * Part 3 ([ExtendedCatalogPage3]) has the `xContainer`/`onXContainer` colours and the contrast
 * chips. Three parts, not two, because at the Android 200% font-scale check two of the three
 * 14-token groups together already overflow the smallest viewport. */
@Composable
internal fun ExtendedCatalogPage2() {
    val colors = RepzTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        CatalogSectionLabel("semantic base colours (x / onX)")
        ColorSwatchGrid(entries = semanticBaseEntries(colors), columns = 3)
    }
}

private fun semanticBaseEntries(colors: RepzColors) =
    listOf(
        ColorEntry("success", colors.success),
        ColorEntry("onSuccess", colors.onSuccess),
        ColorEntry("warning", colors.warning),
        ColorEntry("onWarning", colors.onWarning),
        ColorEntry("record", colors.record),
        ColorEntry("onRecord", colors.onRecord),
        ColorEntry("setWarmup", colors.setWarmup),
        ColorEntry("onSetWarmup", colors.onSetWarmup),
        ColorEntry("setNormal", colors.setNormal),
        ColorEntry("onSetNormal", colors.onSetNormal),
        ColorEntry("setDrop", colors.setDrop),
        ColorEntry("onSetDrop", colors.onSetDrop),
        ColorEntry("setFailure", colors.setFailure),
        ColorEntry("onSetFailure", colors.onSetFailure),
    )
