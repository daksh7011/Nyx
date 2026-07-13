package com.slothiesmooth.nyx.designlibrary.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slothiesmooth.nyx.designlibrary.atoms.NxButton
import com.slothiesmooth.nyx.designlibrary.molecules.NxTopBar
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
