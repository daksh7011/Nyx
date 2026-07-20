package com.slothiesmooth.nyx.androidapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.slothiesmooth.nyx.shared.data.source.ClipboardWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val CLIP_LABEL = "nyx"

/** Writes to the Android system clipboard via [ClipboardManager] (replaces the deprecated Compose one). */
class AndroidClipboardWriter(private val context: Context) : ClipboardWriter {

    override suspend fun copyPlainText(text: String) = withContext(Dispatchers.Main) {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText(CLIP_LABEL, text))
    }
}
