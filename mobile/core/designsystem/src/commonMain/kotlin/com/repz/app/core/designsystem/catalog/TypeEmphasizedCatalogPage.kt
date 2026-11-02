package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.repz.app.core.designsystem.theme.RepzSpacing

/** The 15 emphasized M3 styles (DESIGN_SYSTEM §4, Q2): same layout as [TypeCatalogPage], so the
 * two pages can be compared side by side (display/headline visibly wider and bolder). */
@Composable
internal fun TypeEmphasizedCatalogPage() {
    val typography = MaterialTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        TypeStyleRow("displayLargeEmphasized", typography.displayLargeEmphasized)
        TypeStyleRow("displayMediumEmphasized", typography.displayMediumEmphasized)
        TypeStyleRow("displaySmallEmphasized", typography.displaySmallEmphasized)
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
