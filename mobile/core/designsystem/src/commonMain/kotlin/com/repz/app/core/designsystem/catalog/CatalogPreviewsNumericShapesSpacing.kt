package com.repz.app.core.designsystem.catalog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzTheme

@Preview
@Composable
private fun RepzDesignSystemCatalogNumericLightPreview() {
    RepzTheme(darkTheme = false) { RepzDesignSystemCatalog(RepzCatalogPage.Numeric) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogNumericDarkPreview() {
    RepzTheme(darkTheme = true) { RepzDesignSystemCatalog(RepzCatalogPage.Numeric) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogShapesLightPreview() {
    RepzTheme(darkTheme = false) { RepzDesignSystemCatalog(RepzCatalogPage.Shapes) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogShapesDarkPreview() {
    RepzTheme(darkTheme = true) { RepzDesignSystemCatalog(RepzCatalogPage.Shapes) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogSpacingElevationLightPreview() {
    RepzTheme(darkTheme = false) { RepzDesignSystemCatalog(RepzCatalogPage.SpacingElevation) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogSpacingElevationDarkPreview() {
    RepzTheme(darkTheme = true) { RepzDesignSystemCatalog(RepzCatalogPage.SpacingElevation) }
}
