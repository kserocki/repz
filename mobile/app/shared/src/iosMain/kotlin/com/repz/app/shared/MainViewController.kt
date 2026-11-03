package com.repz.app.shared

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * The single entry point Swift calls
 * (`MainViewControllerKt.mainViewController(catalogPageKey:catalogReducedMotion:)`) to host the
 * shared Compose UI inside `ComposeView`'s `UIViewControllerRepresentable`.
 *
 * [catalogPageKey] is the debug-only design-system catalog hook ([RepzRoot]'s `catalogPageKey`).
 * `ComposeView.swift` reads it from a user default only under `#if DEBUG` and passes a literal
 * `nil` otherwise, so release builds never resolve a [com.repz.app.core.designsystem.catalog.RepzCatalogPage]
 * and always render the normal placeholder. Kotlin default parameter values are not visible from
 * Objective-C, so this function takes no default and Swift must always pass a value explicitly.
 *
 * [catalogReducedMotion] is the debug-only "force reduced motion" hook ([RepzRoot]'s
 * `catalogReducedMotion`). A plain (non-nullable) `Boolean` avoids bridging an `NSNumber?` to
 * Swift: `false` maps to `null` (follow the live platform setting) and `true` forces it, matching
 * what `-RepzReducedMotion YES`/absent does in `ComposeView.swift`.
 *
 * [debugSignedIn] is the debug-only "treat the session as signed in" hook ([RepzRoot]'s
 * `debugSignedIn`). `ComposeView.swift` reads it from a user default only under `#if DEBUG` and
 * passes a literal `false` otherwise, so release builds never bypass the real session state. No
 * default value, for the same reason as [catalogReducedMotion].
 */
fun mainViewController(
    catalogPageKey: String?,
    catalogReducedMotion: Boolean,
    debugSignedIn: Boolean,
): UIViewController =
    ComposeUIViewController {
        RepzRoot(
            catalogPageKey = catalogPageKey,
            catalogReducedMotion = if (catalogReducedMotion) true else null,
            debugSignedIn = debugSignedIn,
        )
    }
