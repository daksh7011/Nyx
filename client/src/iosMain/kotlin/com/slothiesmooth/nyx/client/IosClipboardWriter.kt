package com.slothiesmooth.nyx.client

import com.slothiesmooth.nyx.shared.data.source.ClipboardWriter
import platform.UIKit.UIPasteboard

/** Writes to the iOS system clipboard via [UIPasteboard]. */
class IosClipboardWriter : ClipboardWriter {
    override suspend fun copyPlainText(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }
}
