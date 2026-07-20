package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions
import com.slothiesmooth.nyx.designlibrary.tokens.nxType

private val FieldTextSize = 15.sp
private val MultilineMinHeight = 80.dp
private const val MULTILINE_MIN_LINES = 3

@Composable
fun NxField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    multiline: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.nxColors
    val type = MaterialTheme.nxType
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label.uppercase(), style = type.kicker, color = colors.fgSubtle) },
        placeholder = if (placeholder.isNotEmpty()) {
            { Text(placeholder, style = type.body, color = colors.fgFaint) }
        } else {
            null
        },
        singleLine = !multiline,
        minLines = if (multiline) MULTILINE_MIN_LINES else 1,
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.brand,
            unfocusedBorderColor = colors.borderStrong,
            focusedTextColor = colors.fg,
            unfocusedTextColor = colors.fg,
            focusedLabelColor = colors.brand,
            unfocusedLabelColor = colors.fgSubtle,
            cursorColor = colors.brand,
            focusedContainerColor = colors.bgElev1,
            unfocusedContainerColor = colors.bgElev1,
        ),
        textStyle = type.body.copy(fontSize = FieldTextSize),
        modifier = if (multiline) modifier.heightIn(min = MultilineMinHeight) else modifier,
    )
}

private val SampleWidth = 360.dp

@Composable
fun NxFieldSample() {
    Column(
        modifier = Modifier.padding(MaterialTheme.nxDimensions.keyline4).width(SampleWidth),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline3),
    ) {
        NxFieldTextStates()
        NxFieldPasswordStates()
    }
}

@Composable
private fun NxFieldTextStates() {
    // Single-line, filled, enabled.
    NxField(
        value = "vacation-2026.png",
        onValueChange = {},
        label = "Image name",
        modifier = Modifier.fillMaxWidth(),
    )
    // Single-line, empty, placeholder shown, enabled.
    NxField(
        value = "",
        onValueChange = {},
        label = "Passphrase hint",
        placeholder = "A clue only you know",
        modifier = Modifier.fillMaxWidth(),
    )
    // Single-line, empty, no placeholder (label only), enabled.
    NxField(
        value = "",
        onValueChange = {},
        label = "Recipient",
        modifier = Modifier.fillMaxWidth(),
    )
    // Multi-line, filled, enabled.
    NxField(
        value = "Meet at the old pier after dusk.\nBring the second key.\nDestroy this note.",
        onValueChange = {},
        label = "Message",
        multiline = true,
        modifier = Modifier.fillMaxWidth(),
    )
    // Multi-line, empty, placeholder shown, enabled.
    NxField(
        value = "",
        onValueChange = {},
        label = "Draft",
        placeholder = "The secret to hide",
        multiline = true,
        modifier = Modifier.fillMaxWidth(),
    )
    // Single-line, filled, disabled.
    NxField(
        value = "read only",
        onValueChange = {},
        label = "Disabled filled",
        enabled = false,
        modifier = Modifier.fillMaxWidth(),
    )
    // Single-line, empty, placeholder shown, disabled.
    NxField(
        value = "",
        onValueChange = {},
        label = "Disabled empty",
        placeholder = "Unavailable",
        enabled = false,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun NxFieldPasswordStates() {
    // Masked, filled, enabled (with visibility toggle).
    NxPasswordField(
        value = "hunter2hunter2",
        onValueChange = {},
        label = "Password",
        modifier = Modifier.fillMaxWidth(),
    )
    // Masked, empty, placeholder shown, enabled.
    NxPasswordField(
        value = "",
        onValueChange = {},
        label = "Confirm password",
        placeholder = "Repeat it exactly",
        modifier = Modifier.fillMaxWidth(),
    )
    // Masked, filled, disabled.
    NxPasswordField(
        value = "hunter2hunter2",
        onValueChange = {},
        label = "Disabled password",
        enabled = false,
        modifier = Modifier.fillMaxWidth(),
    )
}

@AllThemePreview
@Composable
private fun NxFieldPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxFieldSample() }
}
