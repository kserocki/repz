package com.repz.app.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.RepzBrandColors
import com.repz.app.core.designsystem.catalog.RepzCatalogPage
import com.repz.app.core.designsystem.catalog.RepzDesignSystemCatalog
import com.repz.app.core.designsystem.theme.RepzTheme

/**
 * The app's root composable, hosted by both platform shells right after Koin has started.
 *
 * Fills the window with the plain brand background (paper by day, ink by night) that the static
 * OS splash already showed, so the hand-off is a flat colour match with no flash and no symbol.
 *
 * [catalogPageKey] is a debug-only hook into the design-system catalog: when it resolves to a
 * known [RepzCatalogPage], the catalog renders instead of the placeholder, themed with
 * [catalogDynamicColor]. The platform shells read this from a debug-only intent extra / launch
 * argument and pass null in release builds, so the normal path below is unchanged.
 *
 * [catalogReducedMotion] is a second debug-only hook, forwarded as [RepzTheme]'s `reducedMotion`:
 * `null` (the default) follows the live platform setting, `true`/`false` forces it. It only ever
 * reaches [RepzTheme] when the catalog is showing, so the normal launch path is unaffected.
 *
 * TODO(REP-28): replace the placeholder with the NavDisplay app shell.
 */
@Composable
fun RepzRoot(
    modifier: Modifier = Modifier,
    catalogPageKey: String? = null,
    catalogDynamicColor: Boolean = false,
    catalogReducedMotion: Boolean? = null,
) {
    val catalogPage = RepzCatalogPage.fromKey(catalogPageKey)
    if (catalogPage != null) {
        RepzTheme(dynamicColor = catalogDynamicColor, reducedMotion = catalogReducedMotion) {
            RepzDesignSystemCatalog(page = catalogPage, modifier = modifier)
        }
    } else {
        LaunchPlaceholder(darkTheme = isSystemInDarkTheme(), modifier = modifier)
    }
}

@Composable
private fun LaunchPlaceholder(
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (darkTheme) RepzBrandColors.Ink else RepzBrandColors.Paper
    Box(modifier = modifier.fillMaxSize().background(backgroundColor))
}

@Preview
@Composable
private fun LaunchPlaceholderLightPreview() {
    LaunchPlaceholder(darkTheme = false)
}

@Preview
@Composable
private fun LaunchPlaceholderDarkPreview() {
    LaunchPlaceholder(darkTheme = true)
}
