package com.repz.app.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.repz.app.core.designsystem.catalog.RepzCatalogPage
import com.repz.app.core.designsystem.catalog.RepzDesignSystemCatalog
import com.repz.app.core.designsystem.theme.RepzTheme
import com.repz.app.shared.shell.RepzAppShell

/**
 * The app's root composable, hosted by both platform shells right after Koin has started.
 *
 * [catalogPageKey] is a debug-only hook into the design-system catalog: when it resolves to a
 * known [RepzCatalogPage], the catalog renders instead of the app shell, themed with
 * [catalogDynamicColor]. The platform shells read this from a debug-only intent extra / launch
 * argument and pass null in release builds, so the normal path below is unchanged.
 *
 * [debugReducedMotion] is a second debug-only hook, forwarded as [RepzTheme]'s `reducedMotion` on
 * both paths: `null` (the default) follows the live platform setting, `true`/`false` forces it.
 * The debug catalog uses it to pin a page's motion state for inspection; the app path uses it so
 * Spotter can force reduced motion on the launch hand-off without a device setting.
 *
 * [debugSignedIn] is a third debug-only hook (ADR-0025): `true` makes the auth gate
 * ([com.repz.app.shared.shell.AuthGate]) treat the session as signed in without any stored
 * tokens, so the signed-in shell can be reached by hand before a real login flow exists. The
 * platform shells read it the same way as the other two hooks and pass `false` in release builds.
 */
@Composable
fun RepzRoot(
    modifier: Modifier = Modifier,
    catalogPageKey: String? = null,
    catalogDynamicColor: Boolean = false,
    debugReducedMotion: Boolean? = null,
    debugSignedIn: Boolean = false,
) {
    val catalogPage = RepzCatalogPage.fromKey(catalogPageKey)
    if (catalogPage != null) {
        RepzTheme(dynamicColor = catalogDynamicColor, reducedMotion = debugReducedMotion) {
            RepzDesignSystemCatalog(page = catalogPage, modifier = modifier)
        }
    } else {
        RepzTheme(reducedMotion = debugReducedMotion) {
            RepzAppShell(debugSignedIn = debugSignedIn, modifier = modifier)
        }
    }
}
