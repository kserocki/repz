package com.repz.app.core.designsystem.catalog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzTheme

@Preview
@Composable
private fun RepzDesignSystemCatalogTypeEmphasized1LightPreview() {
    RepzTheme(darkTheme = false) { RepzDesignSystemCatalog(RepzCatalogPage.TypeEmphasized1) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogTypeEmphasized1DarkPreview() {
    RepzTheme(darkTheme = true) { RepzDesignSystemCatalog(RepzCatalogPage.TypeEmphasized1) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogTypeEmphasized2LightPreview() {
    RepzTheme(darkTheme = false) { RepzDesignSystemCatalog(RepzCatalogPage.TypeEmphasized2) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogTypeEmphasized2DarkPreview() {
    RepzTheme(darkTheme = true) { RepzDesignSystemCatalog(RepzCatalogPage.TypeEmphasized2) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogNumeric1LightPreview() {
    RepzTheme(darkTheme = false) { RepzDesignSystemCatalog(RepzCatalogPage.Numeric1) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogNumeric1DarkPreview() {
    RepzTheme(darkTheme = true) { RepzDesignSystemCatalog(RepzCatalogPage.Numeric1) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogNumeric2LightPreview() {
    RepzTheme(darkTheme = false) { RepzDesignSystemCatalog(RepzCatalogPage.Numeric2) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogNumeric2DarkPreview() {
    RepzTheme(darkTheme = true) { RepzDesignSystemCatalog(RepzCatalogPage.Numeric2) }
}
