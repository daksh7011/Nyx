package com.slothiesmooth.nyx.androidapp

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import com.slothiesmooth.nyx.shared.data.source.ClipboardWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val CLIP_LABEL = "nyx"

// Pre-API-33 literal for ClipDescription.EXTRA_IS_SENSITIVE (which only exists on Android 13+).
private const val EXTRA_IS_SENSITIVE_COMPAT = "android.content.extra.IS_SENSITIVE"

/** Writes to the Android system clipboard via [ClipboardManager] (replaces the deprecated Compose one). */
class AndroidClipboardWriter(private val context: Context) : ClipboardWriter {

    override suspend fun copyPlainText(text: String) = withContext(Dispatchers.Main) {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(CLIP_LABEL, text).apply {
            // Copied content may be a decrypted secret: flag it sensitive so Android 13+ redacts the
            // clipboard preview and keeps it out of the keyboard clipboard history.
            val sensitiveKey = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ClipDescription.EXTRA_IS_SENSITIVE
            } else {
                EXTRA_IS_SENSITIVE_COMPAT
            }
            description.extras = PersistableBundle().apply { putBoolean(sensitiveKey, true) }
        }
        manager.setPrimaryClip(clip)
    }
}
