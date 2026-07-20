package com.slothiesmooth.nyx.shared.testsupport

import com.slothiesmooth.nyx.shared.data.source.ClipboardWriter

/** In-memory [ClipboardWriter]; records the last copied text for assertions. */
class FakeClipboardWriter : ClipboardWriter {
    var lastCopied: String? = null

    override suspend fun copyPlainText(text: String) {
        lastCopied = text
    }
}
