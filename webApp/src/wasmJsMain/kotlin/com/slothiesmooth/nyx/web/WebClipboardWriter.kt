package com.slothiesmooth.nyx.web

import com.slothiesmooth.nyx.shared.data.source.ClipboardWriter
import kotlinx.browser.window

/** Copies text to the browser clipboard via the async Clipboard API. */
class WebClipboardWriter : ClipboardWriter {
    override suspend fun copyPlainText(text: String) {
        window.navigator.clipboard.writeText(text)
    }
}
