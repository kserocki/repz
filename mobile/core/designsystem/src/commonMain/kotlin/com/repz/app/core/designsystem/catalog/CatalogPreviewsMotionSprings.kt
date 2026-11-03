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
private fun RepzDesignSystemCatalogMotionTweensLightPreview() {
    RepzTheme(darkTheme = false) { RepzDesignSystemCatalog(RepzCatalogPage.MotionTweens) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogMotionTweensDarkPreview() {
    RepzTheme(darkTheme = true) { RepzDesignSystemCatalog(RepzCatalogPage.MotionTweens) }
}

@Preview
@Composable
private fun RepzDesignSystemCatalogMotionTweensReducedMotionPreview() {
    RepzTheme(darkTheme = false, reducedMotion = true) { RepzDesignSystemCatalog(RepzCatalogPage.MotionTweens) }
}
