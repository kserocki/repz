package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.repz.app.core.designsystem.theme.RepzSpacing

/** Part 1 of 2 of the 15 emphasized M3 styles (DESIGN_SYSTEM §4): display, the widest/boldest
 * group. Its long `*Emphasized` names wrap 2–3 lines each at `wdth 112`, so only this group (not
 * headline too) fits the smallest test viewport. Same layout as [TypeCatalogPage], so the two can
 * be compared side by side (visibly wider and bolder). Part 2 is [TypeEmphasizedCatalogPage2]. */
@Composable
internal fun TypeEmphasizedCatalogPage1() {
    val typography = MaterialTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        TypeStyleRow("displayLargeEmphasized", typography.displayLargeEmphasized)
        TypeStyleRow("displayMediumEmphasized", typography.displayMediumEmphasized)
        TypeStyleRow("displaySmallEmphasized", typography.displaySmallEmphasized)
    }
}
