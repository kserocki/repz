package com.repz.app.core.designsystem.catalog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzTheme

@Preview
@Composable
private fun RepzDesignSystemCatalogMotionSprings1LightPreview() {
    RepzTheme(darkTheme = false) { RepzDesignSystemCatalog(RepzCatalogPage.MotionSprings1) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogMotionSprings1DarkPreview() {
    RepzTheme(darkTheme = true) { RepzDesignSystemCatalog(RepzCatalogPage.MotionSprings1) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogMotionSprings1ReducedMotionPreview() {
    RepzTheme(darkTheme = false, reducedMotion = true) { RepzDesignSystemCatalog(RepzCatalogPage.MotionSprings1) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogMotionSprings2LightPreview() {
    RepzTheme(darkTheme = false) { RepzDesignSystemCatalog(RepzCatalogPage.MotionSprings2) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogMotionSprings2DarkPreview() {
    RepzTheme(darkTheme = true) { RepzDesignSystemCatalog(RepzCatalogPage.MotionSprings2) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogMotionSprings2ReducedMotionPreview() {
    RepzTheme(darkTheme = false, reducedMotion = true) { RepzDesignSystemCatalog(RepzCatalogPage.MotionSprings2) }
}

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
