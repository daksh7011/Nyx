package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions

private val SampleWidth = 360.dp

@Composable
fun NxButtonSample() {
    Column(
        modifier = Modifier.padding(MaterialTheme.nxDimensions.keyline4).width(SampleWidth),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline3),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline2)) {
            NxButton(text = "Primary", onClick = {}, style = NxButtonStyle.Primary)
            NxButton(text = "Soft", onClick = {}, style = NxButtonStyle.Soft)
            NxButton(text = "Ghost", onClick = {}, style = NxButtonStyle.Ghost)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline2)) {
            NxButton(text = "Small", onClick = {}, style = NxButtonStyle.Primary, size = NxButtonSize.Small)
            NxButton(text = "Small soft", onClick = {}, style = NxButtonStyle.Soft, size = NxButtonSize.Small)
            NxButton(text = "Small ghost", onClick = {}, style = NxButtonStyle.Ghost, size = NxButtonSize.Small)
        }
        NxButton(
            text = "Encrypt message",
            onClick = {},
            style = NxButtonStyle.Primary,
            block = true,
            leadingIcon = NxIconKind.Lock,
        )
        NxButton(text = "Wipe vault", onClick = {}, style = NxButtonStyle.Danger, leadingIcon = NxIconKind.Trash)
        NxButton(text = "Disabled", onClick = {}, style = NxButtonStyle.Primary, enabled = false)
    }
}

@AllThemePreview
@Composable
private fun NxButtonPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxButtonSample() }
}
