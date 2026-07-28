package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions

private const val SCRIM_ALPHA = 0.72f

/**
 * A blocking progress scrim: a translucent [background] over the content it covers, centered on a
 * brand spinner and a [label]. Size is caller-driven (typically `Modifier.fillMaxSize()`).
 */
@Composable
fun NxProgressOverlay(
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.nxColors
    Box(
        modifier = modifier.background(colors.bg.copy(alpha = SCRIM_ALPHA)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline3),
        ) {
            CircularProgressIndicator(color = colors.brand)
            NxText(text = label, style = NxTextStyle.Caption, color = colors.fgMuted)
        }
    }
}

@Composable
fun NxProgressOverlaySample() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline4),
    ) {
        // Short, single-line label over covered content: the common blocking case.
        NxProgressOverlayScene(label = "Encrypting")
        // Long label that wraps to multiple lines: exercises multi-line centering.
        NxProgressOverlayScene(label = "Sealing your message with an end-to-end encrypted key")
    }
}

/** A fixed-height panel that draws sample content behind the translucent overlay. */
@Composable
private fun NxProgressOverlayScene(label: String) {
    val colors = MaterialTheme.nxColors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MaterialTheme.nxDimensions.keyline32)
            .background(colors.bgElev1),
    ) {
        NxProgressOverlayBackdrop()
        NxProgressOverlay(label = label, modifier = Modifier.fillMaxSize())
    }
}

/** Covered content, dimmed by the scrim so the overlay's translucency is visible in the golden. */
@Composable
private fun NxProgressOverlayBackdrop() {
    val colors = MaterialTheme.nxColors
    Column(
        modifier = Modifier.fillMaxSize().padding(MaterialTheme.nxDimensions.keyline4),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline2),
    ) {
        NxText(text = "Vault", style = NxTextStyle.Heading, color = colors.fg)
        NxText(text = "Three sealed notes", style = NxTextStyle.Body, color = colors.fgMuted)
        NxText(text = "Tap to reveal", style = NxTextStyle.Caption, color = colors.fgSubtle)
    }
}

@AllThemePreview
@Composable
private fun NxProgressOverlayPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxProgressOverlaySample() }
}
