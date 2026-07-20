package com.slothiesmooth.nyx.designlibrary.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.slothiesmooth.nyx.designlibrary.atoms.NxButton
import com.slothiesmooth.nyx.designlibrary.atoms.NxField
import com.slothiesmooth.nyx.designlibrary.atoms.NxPasswordField
import com.slothiesmooth.nyx.designlibrary.molecules.NxTopBar
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions

/**
 * A single-submit form scaffold: an [NxTopBar] over a scrolling, padded [content] column of fields,
 * with a pinned full-width primary action ([primaryLabel] + [onPrimary], gated by [primaryEnabled])
 * anchored at the bottom.
 */
@Composable
fun NxFormTemplate(
    title: String,
    onBack: () -> Unit,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    primaryEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.nxColors
    val dimensions = MaterialTheme.nxDimensions
    Column(modifier = modifier.fillMaxSize().background(colors.bg)) {
        NxTopBar(title = title, onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(dimensions.keyline4),
            verticalArrangement = Arrangement.spacedBy(dimensions.keyline3),
            content = content,
        )
        Column(modifier = Modifier.fillMaxWidth().padding(dimensions.keyline4)) {
            NxButton(text = primaryLabel, onClick = onPrimary, block = true, enabled = primaryEnabled)
        }
    }
}

/**
 * A filled, ready-to-submit form: every field carries a value and the primary action is enabled.
 * Exercises single-line, multiline, and password field content inside the scaffold.
 */
@Composable
private fun NxFormTemplateFilledSample(modifier: Modifier = Modifier) {
    NxFormTemplate(
        title = "Encrypt",
        onBack = {},
        primaryLabel = "Encrypt message",
        onPrimary = {},
        primaryEnabled = true,
        modifier = modifier,
    ) {
        NxField(
            value = "vacation-2026.png",
            onValueChange = {},
            label = "Image name",
            modifier = Modifier.fillMaxWidth(),
        )
        NxField(
            value = "Meet me at the old pier at midnight.",
            onValueChange = {},
            label = "Message",
            multiline = true,
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

/**
 * A pristine form: every field is empty and shows its placeholder, and the primary action is disabled
 * because required input is missing. Mirrors [NxFormTemplateFilledSample] to isolate the empty and
 * disabled states.
 */
@Composable
private fun NxFormTemplateEmptySample(modifier: Modifier = Modifier) {
    NxFormTemplate(
        title = "Encrypt",
        onBack = {},
        primaryLabel = "Encrypt message",
        onPrimary = {},
        primaryEnabled = false,
        modifier = modifier,
    ) {
        NxField(
            value = "",
            onValueChange = {},
            label = "Image name",
            placeholder = "vacation-2026.png",
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
        NxPasswordField(
            value = "",
            onValueChange = {},
            label = "Password",
            placeholder = "Choose a strong password",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun NxFormTemplateSample() {
    Row(modifier = Modifier.fillMaxSize()) {
        NxFormTemplateFilledSample(modifier = Modifier.weight(1f))
        NxFormTemplateEmptySample(modifier = Modifier.weight(1f))
    }
}

@AllThemePreview
@Composable
private fun NxFormTemplatePaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxFormTemplateSample() }
}
