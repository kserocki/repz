package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

/**
 * Part 1 of 2 of the 4 numeric styles (DESIGN_SYSTEM §4): `numericHero`/`numericLarge`, plus
 * `numericMedium`'s stacked [CatalogSpecimens.TIMER_STACK] proving tabular-figure alignment. Split
 * out so the page fits the smallest test viewport, including the Android 200% font-scale check.
 * Part 2 ([NumericCatalogPage2]) has `numericSmall` and the PL diacritic lines.
 */
@Composable
internal fun NumericCatalogPage1() {
    val numeric = RepzTheme.typography
    val onSurface = MaterialTheme.colorScheme.onSurface

    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.md)) {
        CatalogSectionLabel("numericHero")
        Text(CatalogSpecimens.TIMER_HERO, style = numeric.numericHero, color = onSurface)

        CatalogSectionLabel("numericLarge")
        Text(CatalogSpecimens.WEIGHT_SAMPLE, style = numeric.numericLarge, color = onSurface)

        CatalogSectionLabel("numericMedium (tabular-figure alignment)")
        Column {
            CatalogSpecimens.TIMER_STACK.forEach { value ->
                Text(value, style = numeric.numericMedium, color = onSurface)
            }
        }
    }
}
