package com.slothiesmooth.nyx.designlibrary.templates

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slothiesmooth.nyx.designlibrary.atoms.NxField
import com.slothiesmooth.nyx.designlibrary.atoms.NxPasswordField
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

@Composable
fun NxFormTemplateSample() {
    NxFormTemplate(
        title = "Encrypt",
        onBack = {},
        primaryLabel = "Encrypt message",
        onPrimary = {},
    ) {
        NxField(
            value = "vacation-2026.png",
            onValueChange = {},
            label = "Image name",
            modifier = Modifier.fillMaxWidth(),
        )
        NxPasswordField(
            value = "hunter2hunter2",
            onValueChange = {},
            label = "Password",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@AllThemePreview
@Composable
private fun NxFormTemplatePaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxFormTemplateSample() }
}
