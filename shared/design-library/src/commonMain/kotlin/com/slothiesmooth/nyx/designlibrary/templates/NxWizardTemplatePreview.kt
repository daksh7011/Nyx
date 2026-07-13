package com.slothiesmooth.nyx.designlibrary.templates

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.slothiesmooth.nyx.designlibrary.atoms.NxField
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import kotlinx.collections.immutable.persistentListOf

@Composable
fun NxWizardTemplateSample() {
    NxWizardTemplate(
        stepLabels = persistentListOf("Image", "Message", "Done"),
        currentStep = 1,
        title = "Encrypt",
        onBack = {},
    ) {
        NxText(
            text = "Write the secret you want to hide.",
            style = NxTextStyle.Body,
            color = MaterialTheme.nxColors.fgMuted,
        )
        NxField(
            value = "",
            onValueChange = {},
            label = "Message",
            placeholder = "The secret to hide",
            multiline = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@AllThemePreview
@Composable
private fun NxWizardTemplatePaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxWizardTemplateSample() }
}
