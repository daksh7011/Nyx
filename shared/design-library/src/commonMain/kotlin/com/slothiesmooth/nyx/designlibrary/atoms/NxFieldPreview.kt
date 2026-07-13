package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions

private val SampleWidth = 360.dp

@Composable
fun NxFieldSample() {
    Column(
        modifier = Modifier.padding(MaterialTheme.nxDimensions.keyline4).width(SampleWidth),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline3),
    ) {
        NxField(
            value = "vacation-2026.png",
            onValueChange = {},
            label = "Image name",
            modifier = Modifier.fillMaxWidth(),
        )
        NxField(
            value = "",
            onValueChange = {},
            label = "Message",
            placeholder = "The secret to hide",
            multiline = true,
            modifier = Modifier.fillMaxWidth(),
        )
        NxField(
            value = "read only",
            onValueChange = {},
            label = "Disabled",
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        )
        NxPasswordField(
            value = "hunter2hunter2",
            onValueChange = {},
            label = "Password",
            modifier = Modifier.fillMaxWidth(),
        )
        NxPasswordField(
            value = "",
            onValueChange = {},
            label = "Confirm password",
            placeholder = "Repeat it exactly",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@AllThemePreview
@Composable
private fun NxFieldPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxFieldSample() }
}
