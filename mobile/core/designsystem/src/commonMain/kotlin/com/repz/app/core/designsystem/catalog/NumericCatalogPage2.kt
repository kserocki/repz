package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

/**
 * Part 2 of 2 of the 4 numeric styles (DESIGN_SYSTEM §4): `numericSmall`, plus the PL diacritic
 * line in `titleLarge` and `bodyLarge` to prove both fonts cover Polish glyphs. Part 1
 * ([NumericCatalogPage1]) has `numericHero`/`numericLarge`/`numericMedium`.
 */
@Composable
internal fun NumericCatalogPage2() {
    val numeric = RepzTheme.typography
    val onSurface = MaterialTheme.colorScheme.onSurface

    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.md)) {
        CatalogSectionLabel("numericSmall")
        Text(CatalogSpecimens.VOLUME_SAMPLE, style = numeric.numericSmall, color = onSurface)

        CatalogSectionLabel("PL diacritics – titleLarge")
        Text(CatalogSpecimens.PL_DIACRITIC_LINE, style = MaterialTheme.typography.titleLarge, color = onSurface)

        CatalogSectionLabel("PL diacritics – bodyLarge")
        Text(CatalogSpecimens.PL_DIACRITIC_LINE, style = MaterialTheme.typography.bodyLarge, color = onSurface)
    }
}
