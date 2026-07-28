package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions
import com.slothiesmooth.nyx.designlibrary.tokens.nxType

@Composable
fun NxText(
    text: String,
    modifier: Modifier = Modifier,
    style: NxTextStyle = NxTextStyle.Body,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null,
) {
    val resolvedColor = if (color == Color.Unspecified) MaterialTheme.nxColors.fg else color
    val textStyle = when (style) {
        NxTextStyle.Display -> MaterialTheme.nxType.display
        NxTextStyle.Title -> MaterialTheme.nxType.title
        NxTextStyle.Heading -> MaterialTheme.nxType.heading
        NxTextStyle.Subhead -> MaterialTheme.nxType.subhead
        NxTextStyle.Body -> MaterialTheme.nxType.body
        NxTextStyle.BodyStrong -> MaterialTheme.nxType.bodyStrong
        NxTextStyle.Caption -> MaterialTheme.nxType.caption
        NxTextStyle.Kicker -> MaterialTheme.nxType.kicker
        NxTextStyle.Mono -> MaterialTheme.nxType.mono
    }
    Text(
        text = if (style == NxTextStyle.Kicker) text.uppercase() else text,
        modifier = modifier,
        color = resolvedColor,
        style = textStyle,
        maxLines = maxLines,
        textAlign = textAlign,
    )
}

private const val NX_TEXT_LONG_SAMPLE =
    "This is a deliberately long paragraph used to exercise wrapping and truncation " +
        "behavior across multiple lines so the golden snapshot captures both states."

@Composable
private fun NxTextStyleShowcase() {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline3)) {
        NxText("Hidden in plain sight.", style = NxTextStyle.Display)
        NxText("Vault", style = NxTextStyle.Title)
        NxText("Encrypt a message", style = NxTextStyle.Heading)
        NxText("Top bar title", style = NxTextStyle.Subhead)
        NxText("Standard body text inside cards.", style = NxTextStyle.Body)
        NxText("Body strong — same size, w600.", style = NxTextStyle.BodyStrong)
        NxText("Subtle line under titles.", style = NxTextStyle.Caption)
        NxText("section label", style = NxTextStyle.Kicker)
        NxText("12 images", style = NxTextStyle.Mono)
    }
}

@Composable
private fun NxTextColorShowcase() {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline2)) {
        NxText("Default foreground", style = NxTextStyle.Body)
        NxText("Muted foreground", style = NxTextStyle.Body, color = MaterialTheme.nxColors.fgMuted)
        NxText("Subtle foreground", style = NxTextStyle.Body, color = MaterialTheme.nxColors.fgSubtle)
        NxText("Violet accent", style = NxTextStyle.Body, color = MaterialTheme.nxColors.accentViolet)
        NxText("Cyan accent", style = NxTextStyle.Body, color = MaterialTheme.nxColors.accentCyan)
        NxText("Amber accent", style = NxTextStyle.Body, color = MaterialTheme.nxColors.accentAmber)
        NxText("Rose accent", style = NxTextStyle.Body, color = MaterialTheme.nxColors.accentRose)
    }
}

@Composable
private fun NxTextAlignmentShowcase() {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline2)) {
        NxText(
            "Start aligned",
            modifier = Modifier.fillMaxWidth(),
            style = NxTextStyle.Body,
            textAlign = TextAlign.Start,
        )
        NxText(
            "Center aligned",
            modifier = Modifier.fillMaxWidth(),
            style = NxTextStyle.Body,
            textAlign = TextAlign.Center,
        )
        NxText(
            "End aligned",
            modifier = Modifier.fillMaxWidth(),
            style = NxTextStyle.Body,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun NxTextTruncationShowcase() {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline2)) {
        NxText(NX_TEXT_LONG_SAMPLE, style = NxTextStyle.Body, maxLines = 1)
        NxText(NX_TEXT_LONG_SAMPLE, style = NxTextStyle.Body, maxLines = 2)
        NxText(NX_TEXT_LONG_SAMPLE, style = NxTextStyle.Body)
    }
}

@Composable
fun NxTextSample() {
    Column(
        modifier = Modifier.padding(MaterialTheme.nxDimensions.keyline4),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline4),
    ) {
        NxText("Styles", style = NxTextStyle.Kicker, color = MaterialTheme.nxColors.fgSubtle)
        NxTextStyleShowcase()
        NxText("Colors", style = NxTextStyle.Kicker, color = MaterialTheme.nxColors.fgSubtle)
        NxTextColorShowcase()
        NxText("Alignment", style = NxTextStyle.Kicker, color = MaterialTheme.nxColors.fgSubtle)
        NxTextAlignmentShowcase()
        NxText("Truncation", style = NxTextStyle.Kicker, color = MaterialTheme.nxColors.fgSubtle)
        NxTextTruncationShowcase()
    }
}

@AllThemePreview
@Composable
private fun NxTextPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxTextSample() }
}
