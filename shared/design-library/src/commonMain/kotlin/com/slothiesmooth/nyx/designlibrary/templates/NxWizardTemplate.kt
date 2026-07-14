package com.slothiesmooth.nyx.designlibrary.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.atoms.NxField
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.molecules.NxTopBar
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

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

/**
 * Bounds a full-screen [NxWizardTemplate] to a fixed preview height so several wizard states can be
 * stacked and compared in one snapshot. The wizard's own `fillMaxSize` resolves to this box.
 */
@Composable
private fun WizardPreviewFrame(content: @Composable () -> Unit) {
    val dimensions = MaterialTheme.nxDimensions
    Box(modifier = Modifier.fillMaxWidth().height(dimensions.keyline32 + dimensions.keyline24)) {
        content()
    }
}

@Composable
private fun WizardStartState() {
    val colors = MaterialTheme.nxColors
    // First step (currentStep = 0), three steps, back present: only badge 1 is branded.
    WizardPreviewFrame {
        NxWizardTemplate(
            stepLabels = persistentListOf("Image", "Message", "Done"),
            currentStep = 0,
            title = "Encrypt",
            onBack = {},
        ) {
            NxText(
                text = "Choose the image you want to hide your secret inside.",
                style = NxTextStyle.Body,
                color = colors.fgMuted,
            )
        }
    }
}

@Composable
private fun WizardMiddleState() {
    val colors = MaterialTheme.nxColors
    // Middle step (currentStep = 1), three steps, back present: badges 1 and 2 branded, a field body.
    WizardPreviewFrame {
        NxWizardTemplate(
            stepLabels = persistentListOf("Image", "Message", "Done"),
            currentStep = 1,
            title = "Encrypt",
            onBack = {},
        ) {
            NxText(
                text = "Write the secret you want to hide.",
                style = NxTextStyle.Body,
                color = colors.fgMuted,
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
}

@Composable
private fun WizardCompleteState() {
    val colors = MaterialTheme.nxColors
    // Final step (currentStep = 2 == last index), three steps: every badge is branded ("complete").
    WizardPreviewFrame {
        NxWizardTemplate(
            stepLabels = persistentListOf("Image", "Message", "Done"),
            currentStep = 2,
            title = "Encrypt",
            onBack = {},
        ) {
            NxText(
                text = "Your secret is sealed inside the image.",
                style = NxTextStyle.Body,
                color = colors.fgMuted,
            )
        }
    }
}

@Composable
private fun WizardNoBackState() {
    val colors = MaterialTheme.nxColors
    // First step with no back affordance (onBack omitted): the top bar has no chevron.
    WizardPreviewFrame {
        NxWizardTemplate(
            stepLabels = persistentListOf("Image", "Message", "Done"),
            currentStep = 0,
            title = "New secret",
        ) {
            NxText(
                text = "This is the first step, so there is nowhere to go back to.",
                style = NxTextStyle.Body,
                color = colors.fgMuted,
            )
        }
    }
}

@Composable
private fun WizardTwoStepState() {
    val colors = MaterialTheme.nxColors
    // Minimal step count: two steps, first active, back present.
    WizardPreviewFrame {
        NxWizardTemplate(
            stepLabels = persistentListOf("Passphrase", "Confirm"),
            currentStep = 0,
            title = "Decrypt",
            onBack = {},
        ) {
            NxText(
                text = "Enter the passphrase for this image.",
                style = NxTextStyle.Body,
                color = colors.fgMuted,
            )
        }
    }
}

@Composable
private fun WizardManyStepsState() {
    val colors = MaterialTheme.nxColors
    // Four steps with long labels that must truncate to a single line; middle step active.
    WizardPreviewFrame {
        NxWizardTemplate(
            stepLabels = persistentListOf(
                "Select image",
                "Compose the hidden message",
                "Choose a passphrase",
                "Review and seal",
            ),
            currentStep = 1,
            title = "Encrypt",
            onBack = {},
        ) {
            NxText(
                text = "Long step labels collapse to a single line so the indicator stays on one row.",
                style = NxTextStyle.Body,
                color = colors.fgMuted,
            )
        }
    }
}

@Composable
fun NxWizardTemplateSample() {
    val dimensions = MaterialTheme.nxDimensions
    Column(verticalArrangement = Arrangement.spacedBy(dimensions.keyline1)) {
        WizardStartState()
        WizardMiddleState()
        WizardCompleteState()
        WizardNoBackState()
        WizardTwoStepState()
        WizardManyStepsState()
    }
}

@AllThemePreview
@Composable
private fun NxWizardTemplatePaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxWizardTemplateSample() }
}
