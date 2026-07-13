package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxType

private val PasswordTextSize = 15.sp

@Composable
fun NxPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
) {
    var visible by remember { mutableStateOf(false) }
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
        singleLine = true,
        enabled = enabled,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            NxIconButton(
                kind = if (visible) NxIconKind.EyeOff else NxIconKind.Eye,
                onClick = { visible = !visible },
                style = NxIconButtonStyle.Ghost,
                contentDescription = if (visible) "Hide password" else "Show password",
            )
        },
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
        textStyle = type.body.copy(fontSize = PasswordTextSize),
        modifier = modifier,
    )
}
