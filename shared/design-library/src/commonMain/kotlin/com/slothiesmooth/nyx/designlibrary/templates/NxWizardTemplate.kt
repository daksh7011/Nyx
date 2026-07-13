package com.slothiesmooth.nyx.designlibrary.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.molecules.NxTopBar
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions
import kotlinx.collections.immutable.ImmutableList

private val StepBadgeSize: Dp = 24.dp

/**
 * A multi-step wizard scaffold: an [NxTopBar] over a numbered step indicator (driven by [stepLabels]
 * and [currentStep]) over a scrolling, padded [content] column for the active step's body.
 */
@Composable
fun NxWizardTemplate(
    stepLabels: ImmutableList<String>,
    currentStep: Int,
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.nxColors
    val dimensions = MaterialTheme.nxDimensions
    Column(modifier = modifier.fillMaxSize().background(colors.bg)) {
        NxTopBar(title = title, onBack = onBack)
        NxWizardSteps(stepLabels = stepLabels, currentStep = currentStep)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(dimensions.keyline4),
            verticalArrangement = Arrangement.spacedBy(dimensions.keyline3),
            content = content,
        )
    }
}

@Composable
private fun NxWizardSteps(
    stepLabels: ImmutableList<String>,
    currentStep: Int,
) {
    val colors = MaterialTheme.nxColors
    val dimensions = MaterialTheme.nxDimensions
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions.keyline4, vertical = dimensions.keyline3),
        horizontalArrangement = Arrangement.spacedBy(dimensions.keyline3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        stepLabels.forEachIndexed { index, label ->
            val active = index <= currentStep
            val badgeColor = if (active) colors.brand else colors.bgElev2
            val badgeTextColor = if (active) colors.brandFg else colors.fgMuted
            val labelColor = if (active) colors.fg else colors.fgMuted
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(dimensions.keyline2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(shape = CircleShape, color = badgeColor, modifier = Modifier.size(StepBadgeSize)) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        NxText(text = (index + 1).toString(), style = NxTextStyle.Caption, color = badgeTextColor)
                    }
                }
                NxText(text = label, style = NxTextStyle.Caption, color = labelColor, maxLines = 1)
            }
        }
    }
}
