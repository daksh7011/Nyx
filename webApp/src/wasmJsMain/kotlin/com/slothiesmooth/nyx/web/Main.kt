package com.slothiesmooth.nyx.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.slothiesmooth.nyx.client.app.presentation.App
import com.slothiesmooth.nyx.client.initKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin(webPlatformModule())
    ComposeViewport {
        App()
    }
}
