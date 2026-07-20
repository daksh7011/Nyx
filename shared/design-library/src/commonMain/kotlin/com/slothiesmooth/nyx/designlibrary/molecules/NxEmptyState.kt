package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.atoms.NxButton
import com.slothiesmooth.nyx.designlibrary.atoms.NxIcon
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions

private val EmptyIconSize: Dp = 40.dp
private val EmptyMaxWidth: Dp = 320.dp

/**
 * Centered empty / placeholder state: a large [icon], a [title], supporting [body], and an optional
 * call-to-action ([ctaText] + [onCta]). Used for the "coming soon" shell stubs and every empty list.
 */
@Composable
fun NxEmptyState(
    icon: NxIconKind,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    ctaText: String? = null,
    onCta: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.nxColors
    Column(
        modifier = modifier.fillMaxSize().padding(MaterialTheme.nxDimensions.keyline6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline3, Alignment.CenterVertically),
    ) {
        NxIcon(kind = icon, tint = colors.fgMuted, size = EmptyIconSize)
        NxText(text = title, style = NxTextStyle.Heading, textAlign = TextAlign.Center)
        NxText(
            text = body,
            style = NxTextStyle.Body,
            color = colors.fgSubtle,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = EmptyMaxWidth),
        )
        if (ctaText != null && onCta != null) {
            NxButton(text = ctaText, onClick = onCta)
        }
    }
}

private val EmptyStatePreviewHeight: Dp = 240.dp

/** Bounds a [NxEmptyState] to a finite height so several variants can stack in one preview. */
@Composable
private fun EmptyStateFrame(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(EmptyStatePreviewHeight)) { content() }
}

@Composable
fun NxEmptyStateSample() {
    Column(
        modifier = Modifier.padding(MaterialTheme.nxDimensions.keyline4),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline4),
    ) {
        // With a call-to-action: icon + title + body + button.
        EmptyStateFrame {
            NxEmptyState(
                icon = NxIconKind.Vault,
                title = "Your vault is empty",
                body = "Hidden messages you save will appear here.",
                ctaText = "Encrypt a message",
                onCta = {},
            )
        }
        // Without a call-to-action: icon + title + body only, longer body to show wrapping.
        EmptyStateFrame {
            NxEmptyState(
                icon = NxIconKind.Archive,
                title = "No archived items",
                body = "Messages you archive are kept out of sight until you choose to restore them.",
            )
        }
    }
}

@AllThemePreview
@Composable
private fun NxEmptyStatePaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxEmptyStateSample() }
}
