package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.atoms.NxIcon
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxElevations

private val FabSize: Dp = 56.dp
private val FabIconSize: Dp = 24.dp

/** A circular floating action button: a brand-filled disk carrying a single [icon]. */
@Composable
fun NxFab(
    icon: NxIconKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val colors = MaterialTheme.nxColors
    val shape = CircleShape
    Surface(
        onClick = onClick,
        shape = shape,
        color = colors.brand,
        contentColor = colors.brandFg,
        modifier = modifier.shadow(MaterialTheme.nxElevations.level3, shape).size(FabSize),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            NxIcon(kind = icon, tint = colors.brandFg, size = FabIconSize, contentDescription = contentDescription)
        }
    }
}
