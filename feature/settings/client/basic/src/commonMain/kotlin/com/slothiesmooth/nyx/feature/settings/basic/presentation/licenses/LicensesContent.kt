package com.slothiesmooth.nyx.feature.settings.basic.presentation.licenses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.molecules.NxCard
import com.slothiesmooth.nyx.designlibrary.molecules.NxCardVariant
import com.slothiesmooth.nyx.designlibrary.templates.NxDetailTemplate
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions
import com.slothiesmooth.nyx.feature.settings.basic.domain.LicenseEntry
import com.slothiesmooth.nyx.feature.settings.basic.domain.nyxLicenses
import com.slothiesmooth.nyx.feature.settings.basic.resources.Res
import com.slothiesmooth.nyx.feature.settings.basic.resources.settings_licenses_title
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource

/**
 * Stateless list of third-party licenses. Rendered as a plain [Column] (not a `LazyColumn`): the
 * enclosing [NxDetailTemplate] is already vertically scrollable, and a lazy list inside a scrollable
 * container with unbounded height crashes at measure time. The list is small and hand-maintained, so
 * a plain column costs nothing.
 */
@Composable
fun LicensesContent(
    entries: ImmutableList<LicenseEntry>,
    onOpenUrl: (String) -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    NxDetailTemplate(title = stringResource(Res.string.settings_licenses_title), onBack = onBack, modifier = modifier) {
        entries.forEach { entry ->
            NxCard(variant = NxCardVariant.Elevated, onClick = { onOpenUrl(entry.url) }) {
                Column(modifier = Modifier.fillMaxWidth().padding(MaterialTheme.nxDimensions.keyline3)) {
                    NxText(text = entry.name, style = NxTextStyle.BodyStrong)
                    NxText(text = entry.license, style = NxTextStyle.Caption, color = MaterialTheme.nxColors.fgMuted)
                }
            }
        }
    }
}

@AllThemePreview
@Composable
private fun LicensesContentPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { LicensesContent(entries = nyxLicenses) }
}
