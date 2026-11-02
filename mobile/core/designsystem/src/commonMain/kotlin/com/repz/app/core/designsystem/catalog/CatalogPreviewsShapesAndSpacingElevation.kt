package com.repz.app.core.designsystem.catalog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzTheme

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
private fun RepzDesignSystemCatalogSpacingElevation1LightPreview() {
    RepzTheme(darkTheme = false) { RepzDesignSystemCatalog(RepzCatalogPage.SpacingElevation1) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogSpacingElevation1DarkPreview() {
    RepzTheme(darkTheme = true) { RepzDesignSystemCatalog(RepzCatalogPage.SpacingElevation1) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogSpacingElevation2LightPreview() {
    RepzTheme(darkTheme = false) { RepzDesignSystemCatalog(RepzCatalogPage.SpacingElevation2) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogSpacingElevation2DarkPreview() {
    RepzTheme(darkTheme = true) { RepzDesignSystemCatalog(RepzCatalogPage.SpacingElevation2) }
}
