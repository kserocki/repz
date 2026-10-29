package com.repz.app.shared

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * The single entry point Swift calls (`MainViewControllerKt.mainViewController()`) to host the
 * shared Compose UI inside `ComposeView`'s `UIViewControllerRepresentable`. Lowercase per Detekt's
 * `FunctionNaming` rule (REP-9): this is a plain factory function, not `@Composable`.
 */
fun mainViewController(): UIViewController = ComposeUIViewController { RepzRoot() }
