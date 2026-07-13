package com.slothiesmooth.nyx.designlibrary.templates

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.slothiesmooth.nyx.designlibrary.atoms.NxButton
import com.slothiesmooth.nyx.designlibrary.atoms.NxButtonStyle
import com.slothiesmooth.nyx.designlibrary.atoms.NxIconButton
import com.slothiesmooth.nyx.designlibrary.atoms.NxIconButtonStyle
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors

@Composable
fun NxDetailTemplateSample() {
    NxDetailTemplate(
        title = "vacation-2026.png",
        onBack = {},
        trailing = {
            NxIconButton(
                kind = NxIconKind.Share,
                onClick = {},
                style = NxIconButtonStyle.Ghost,
                contentDescription = "Share",
            )
        },
    ) {
        NxText(text = "Saved 2 hours ago", style = NxTextStyle.Caption, color = MaterialTheme.nxColors.fgMuted)
        NxButton(
            text = "Decrypt this",
            onClick = {},
            block = true,
            leadingIcon = NxIconKind.Unlock,
        )
        NxButton(
            text = "Archive",
            onClick = {},
            style = NxButtonStyle.Soft,
            block = true,
            leadingIcon = NxIconKind.Archive,
        )
    }
}

@AllThemePreview
@Composable
private fun NxDetailTemplatePaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxDetailTemplateSample() }
}
