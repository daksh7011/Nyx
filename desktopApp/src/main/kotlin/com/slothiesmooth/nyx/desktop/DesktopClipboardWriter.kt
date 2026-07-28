package com.slothiesmooth.nyx.desktop

import com.slothiesmooth.nyx.shared.data.source.ClipboardWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/** Writes to the desktop system clipboard via AWT. */
class DesktopClipboardWriter : ClipboardWriter {

    override suspend fun copyPlainText(text: String) = withContext(Dispatchers.IO) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }
}
