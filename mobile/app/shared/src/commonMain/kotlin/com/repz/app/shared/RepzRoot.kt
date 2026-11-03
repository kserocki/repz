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
 * [catalogReducedMotion] is a second debug-only hook, forwarded as [RepzTheme]'s `reducedMotion`:
 * `null` (the default) follows the live platform setting, `true`/`false` forces it. It only ever
 * reaches [RepzTheme] when the catalog is showing, so the normal launch path is unaffected.
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
    catalogReducedMotion: Boolean? = null,
    debugSignedIn: Boolean = false,
) {
    val catalogPage = RepzCatalogPage.fromKey(catalogPageKey)
    if (catalogPage != null) {
        RepzTheme(dynamicColor = catalogDynamicColor, reducedMotion = catalogReducedMotion) {
            RepzDesignSystemCatalog(page = catalogPage, modifier = modifier)
        }
    } else {
        RepzTheme {
            RepzAppShell(debugSignedIn = debugSignedIn, modifier = modifier)
        }
    }
}
