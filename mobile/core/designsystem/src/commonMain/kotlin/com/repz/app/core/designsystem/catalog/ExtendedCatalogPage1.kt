package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.repz.app.core.designsystem.theme.RepzColors
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

/** Part 1 of 3 of every `RepzTheme.colors` token (DESIGN_SYSTEM §3.2): the tokens with no
 * on-colour pair (brand and graphics). Split out so the page fits the smallest test viewport,
 * including the Android 200% font-scale check: at that scale, this group alone already fills most
 * of the screen. Part 2 is [ExtendedCatalogPage2], part 3 is [ExtendedCatalogPage3]. */
@Composable
internal fun ExtendedCatalogPage1() {
    val colors = RepzTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        CatalogSectionLabel("brand / graphics")
        ColorSwatchGrid(entries = plainEntries(colors), columns = 3)
    }
}

private fun plainEntries(colors: RepzColors) =
    listOf(
        ColorEntry("brandCyan", colors.brandCyan),
        ColorEntry("brandDeep", colors.brandDeep),
        ColorEntry("brandInk", colors.brandInk),
        ColorEntry("brandPaper", colors.brandPaper),
        ColorEntry("brandMist", colors.brandMist),
        ColorEntry("recordBright", colors.recordBright),
        ColorEntry("musclePrimary", colors.musclePrimary),
        ColorEntry("muscleSecondary", colors.muscleSecondary),
        ColorEntry("muscleInactive", colors.muscleInactive),
        ColorEntry("chartSeries1", colors.chartSeries1),
        ColorEntry("chartSeries2", colors.chartSeries2),
        ColorEntry("chartSeries3", colors.chartSeries3),
        ColorEntry("chartGrid", colors.chartGrid),
        ColorEntry("chartAxis", colors.chartAxis),
    )
