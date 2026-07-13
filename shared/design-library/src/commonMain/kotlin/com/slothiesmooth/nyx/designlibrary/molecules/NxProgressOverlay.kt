package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions

private const val SCRIM_ALPHA = 0.72f

/**
 * A blocking progress scrim: a translucent [background] over the content it covers, centered on a
 * brand spinner and a [label]. Size is caller-driven (typically `Modifier.fillMaxSize()`).
 */
@Composable
fun NxProgressOverlay(
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.nxColors
    Box(
        modifier = modifier.background(colors.bg.copy(alpha = SCRIM_ALPHA)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline3),
        ) {
            CircularProgressIndicator(color = colors.brand)
            NxText(text = label, style = NxTextStyle.Caption, color = colors.fgMuted)
        }
    }
}
