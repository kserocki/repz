package com.repz.app.core.designsystem.catalog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzTheme

@Preview
@Composable
private fun RepzDesignSystemCatalogMotionMorphsLightPreview() {
    RepzTheme(darkTheme = false) { RepzDesignSystemCatalog(RepzCatalogPage.MotionMorphs) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogMotionMorphsDarkPreview() {
    RepzTheme(darkTheme = true) { RepzDesignSystemCatalog(RepzCatalogPage.MotionMorphs) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogMotionMorphsReducedMotionPreview() {
    RepzTheme(darkTheme = false, reducedMotion = true) { RepzDesignSystemCatalog(RepzCatalogPage.MotionMorphs) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogHapticsLightPreview() {
    RepzTheme(darkTheme = false) { RepzDesignSystemCatalog(RepzCatalogPage.Haptics) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogHapticsDarkPreview() {
    RepzTheme(darkTheme = true) { RepzDesignSystemCatalog(RepzCatalogPage.Haptics) }
}
