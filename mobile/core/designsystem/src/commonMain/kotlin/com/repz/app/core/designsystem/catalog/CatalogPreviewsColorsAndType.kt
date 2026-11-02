package com.repz.app.core.designsystem.catalog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzTheme

@Preview
@Composable
private fun RepzDesignSystemCatalogColorsLightPreview() {
    RepzTheme(darkTheme = false) { RepzDesignSystemCatalog(RepzCatalogPage.Colors) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogColorsDarkPreview() {
    RepzTheme(darkTheme = true) { RepzDesignSystemCatalog(RepzCatalogPage.Colors) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogExtendedLightPreview() {
    RepzTheme(darkTheme = false) { RepzDesignSystemCatalog(RepzCatalogPage.Extended) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogExtendedDarkPreview() {
    RepzTheme(darkTheme = true) { RepzDesignSystemCatalog(RepzCatalogPage.Extended) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogTypeLightPreview() {
    RepzTheme(darkTheme = false) { RepzDesignSystemCatalog(RepzCatalogPage.Type) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogTypeDarkPreview() {
    RepzTheme(darkTheme = true) { RepzDesignSystemCatalog(RepzCatalogPage.Type) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogTypeEmphasizedLightPreview() {
    RepzTheme(darkTheme = false) { RepzDesignSystemCatalog(RepzCatalogPage.TypeEmphasized) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogTypeEmphasizedDarkPreview() {
    RepzTheme(darkTheme = true) { RepzDesignSystemCatalog(RepzCatalogPage.TypeEmphasized) }
}
