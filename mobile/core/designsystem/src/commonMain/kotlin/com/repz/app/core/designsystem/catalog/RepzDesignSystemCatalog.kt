package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

/**
 * The debug-only design-system catalog (DESIGN_SYSTEM §2–7): one page per token group, so Spotter
 * can screenshot every token on real devices. Renders inside the caller's [RepzTheme]. Not
 * user-facing: see [RepzCatalogPage] and [CatalogSpecimens]. Each page has a light and dark
 * `@Preview`, split across several files in this package to stay under Detekt's function-count
 * limit.
 */
@Composable
fun RepzDesignSystemCatalog(
    page: RepzCatalogPage,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(RepzSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg),
    ) {
        CatalogPageTitle(page.key)
        when (page) {
            RepzCatalogPage.Colors -> ColorsCatalogPage()
            RepzCatalogPage.Extended1 -> ExtendedCatalogPage1()
            RepzCatalogPage.Extended2 -> ExtendedCatalogPage2()
            RepzCatalogPage.Extended3 -> ExtendedCatalogPage3()
            RepzCatalogPage.Type -> TypeCatalogPage()
            RepzCatalogPage.TypeEmphasized1 -> TypeEmphasizedCatalogPage1()
            RepzCatalogPage.TypeEmphasized2 -> TypeEmphasizedCatalogPage2()
            RepzCatalogPage.Numeric1 -> NumericCatalogPage1()
            RepzCatalogPage.Numeric2 -> NumericCatalogPage2()
            RepzCatalogPage.Shapes -> ShapesCatalogPage()
            RepzCatalogPage.SpacingElevation1 -> SpacingCatalogPage()
            RepzCatalogPage.SpacingElevation2 -> ElevationCatalogPage()
        }
    }
}
