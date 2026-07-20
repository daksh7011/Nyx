package com.slothiesmooth.nyx.client

import androidx.compose.ui.window.ComposeUIViewController
import com.slothiesmooth.nyx.client.app.presentation.App
import platform.UIKit.UIViewController

/**
 * The iOS entry point: hosts the shared [App] in a Compose UIViewController. Exported to Swift.
 *
 * PascalCase is required by the Kotlin/Native-to-Swift interop contract — the Swift host calls
 * `MainViewControllerKt.MainViewController()`, and this is the universal KMP-iOS entry-point name.
 * Renaming to satisfy FunctionNaming would break that contract, so it is suppressed here only.
 */
@Suppress("FunctionNaming")
fun MainViewController(): UIViewController = ComposeUIViewController {
    App()
}
