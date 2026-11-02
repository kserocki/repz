package com.repz.app.shared

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * The single entry point Swift calls (`MainViewControllerKt.mainViewController(catalogPageKey:)`)
 * to host the shared Compose UI inside `ComposeView`'s `UIViewControllerRepresentable`.
 *
 * [catalogPageKey] is the debug-only design-system catalog hook ([RepzRoot]'s `catalogPageKey`).
 * `ComposeView.swift` reads it from a user default only under `#if DEBUG` and passes a literal
 * `nil` otherwise, so release builds never resolve a [com.repz.app.core.designsystem.catalog.RepzCatalogPage]
 * and always render the normal placeholder. Kotlin default parameter values are not visible from
 * Objective-C, so this function takes no default and Swift must always pass a value explicitly.
 */
fun mainViewController(catalogPageKey: String?): UIViewController = ComposeUIViewController { RepzRoot(catalogPageKey = catalogPageKey) }
