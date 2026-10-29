package com.repz.app.shared

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * The single entry point Swift calls (`MainViewControllerKt.MainViewController()`) to host the
 * shared Compose UI inside `ComposeView`'s `UIViewControllerRepresentable`.
 */
fun MainViewController(): UIViewController = ComposeUIViewController { RepzRoot() }
