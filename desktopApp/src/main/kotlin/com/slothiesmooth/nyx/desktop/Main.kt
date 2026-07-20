package com.slothiesmooth.nyx.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.slothiesmooth.nyx.client.app.presentation.App
import com.slothiesmooth.nyx.client.initKoin
import io.github.vinceglb.filekit.FileKit

fun main() {
    // JVM FileKit needs an appId (a member of FileKit) before FileKit.filesDir is read.
    FileKit.init("Nyx")
    initKoin(desktopPlatformModule())
    application {
        Window(onCloseRequest = ::exitApplication, title = "Nyx") {
            App()
        }
    }
}
