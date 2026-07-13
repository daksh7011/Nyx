package com.slothiesmooth.nyx.designlibrary.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.atoms.NxIcon
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.models.NxBottomNavItem
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions
import kotlinx.collections.immutable.ImmutableList

private val BarHeight: Dp = 64.dp
private val NavIconSize: Dp = 22.dp
private val HairlineWidth: Dp = 1.dp
private val SelectedStripeWidth: Dp = 2.dp

/**
 * The bottom navigation bar: full-width, evenly-weighted [items], each an [NxIcon] over its label.
 * The item at [selectedIndex] is tinted `brand` and carries a top accent stripe; tapping fires
 * [onSelect]. Stateless — selection and routing live in the caller. Fills under the system gesture bar.
 */
@Composable
fun NxBottomNav(
    items: ImmutableList<NxBottomNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.nxColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgElev1)
            .drawBehind {
                drawLine(
                    color = colors.divider,
                    start = Offset(x = 0f, y = 0f),
                    end = Offset(x = size.width, y = 0f),
                    strokeWidth = HairlineWidth.toPx(),
                )
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(BarHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                NxBottomNavCell(
                    item = item,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.fillMaxWidth().windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun NxBottomNavCell(
    item: NxBottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.nxColors
    val tint = if (selected) colors.brand else colors.fgMuted
    val interactionSource = remember { MutableInteractionSource() }
    val stripe = if (selected) {
        Modifier.drawBehind {
            drawLine(
                color = colors.brand,
                start = Offset(x = 0f, y = 0f),
                end = Offset(x = size.width, y = 0f),
                strokeWidth = SelectedStripeWidth.toPx(),
            )
        }
    } else {
        Modifier
    }
    Column(
        modifier = modifier
            .clickable(interactionSource = interactionSource, indication = ripple(), onClick = onClick)
            .then(stripe)
            .padding(vertical = MaterialTheme.nxDimensions.keyline2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline1),
    ) {
        NxIcon(kind = item.icon, tint = tint, contentDescription = item.label, size = NavIconSize)
        NxText(text = item.label, style = NxTextStyle.Kicker, color = tint, maxLines = 1)
    }
}
