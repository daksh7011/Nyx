package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
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
