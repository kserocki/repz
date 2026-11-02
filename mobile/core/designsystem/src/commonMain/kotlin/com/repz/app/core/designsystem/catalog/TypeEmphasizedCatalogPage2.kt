package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.repz.app.core.designsystem.theme.RepzSpacing

/** Part 2 of 2 of the 15 emphasized M3 styles (DESIGN_SYSTEM §4): headline, title, body and label.
 * Part 1 ([TypeEmphasizedCatalogPage1]) has display only. */
@Composable
internal fun TypeEmphasizedCatalogPage2() {
    val typography = MaterialTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        TypeStyleRow("headlineLargeEmphasized", typography.headlineLargeEmphasized)
        TypeStyleRow("headlineMediumEmphasized", typography.headlineMediumEmphasized)
        TypeStyleRow("headlineSmallEmphasized", typography.headlineSmallEmphasized)
        TypeStyleRow("titleLargeEmphasized", typography.titleLargeEmphasized)
        TypeStyleRow("titleMediumEmphasized", typography.titleMediumEmphasized)
        TypeStyleRow("titleSmallEmphasized", typography.titleSmallEmphasized)
        TypeStyleRow("bodyLargeEmphasized", typography.bodyLargeEmphasized)
        TypeStyleRow("bodyMediumEmphasized", typography.bodyMediumEmphasized)
        TypeStyleRow("bodySmallEmphasized", typography.bodySmallEmphasized)
        TypeStyleRow("labelLargeEmphasized", typography.labelLargeEmphasized)
        TypeStyleRow("labelMediumEmphasized", typography.labelMediumEmphasized)
        TypeStyleRow("labelSmallEmphasized", typography.labelSmallEmphasized)
    }
}
