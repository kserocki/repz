package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

/**
 * The 4 numeric styles (DESIGN_SYSTEM §4): [CatalogSpecimens] prove tabular-figure alignment and
 * locale-shaped samples. The PL diacritic line is checked here too, in `titleLarge` and
 * `bodyLarge`, to prove both fonts cover Polish glyphs.
 */
@Composable
internal fun NumericCatalogPage() {
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

        CatalogSectionLabel("numericSmall")
        Text(CatalogSpecimens.VOLUME_SAMPLE, style = numeric.numericSmall, color = onSurface)

        CatalogSectionLabel("PL diacritics – titleLarge")
        Text(CatalogSpecimens.PL_DIACRITIC_LINE, style = MaterialTheme.typography.titleLarge, color = onSurface)

        CatalogSectionLabel("PL diacritics – bodyLarge")
        Text(CatalogSpecimens.PL_DIACRITIC_LINE, style = MaterialTheme.typography.bodyLarge, color = onSurface)
    }
}
