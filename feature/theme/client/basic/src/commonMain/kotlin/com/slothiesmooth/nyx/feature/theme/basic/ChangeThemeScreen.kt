package com.slothiesmooth.nyx.feature.theme.basic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slothiesmooth.nyx.designlibrary.atoms.NxChip
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.molecules.NxCard
import com.slothiesmooth.nyx.designlibrary.molecules.NxCardVariant
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions
import com.slothiesmooth.nyx.feature.theme.api.ThemeMode
import com.slothiesmooth.nyx.feature.theme.basic.resources.Res
import com.slothiesmooth.nyx.feature.theme.basic.resources.theme_mode
import com.slothiesmooth.nyx.feature.theme.basic.resources.theme_mode_dark
import com.slothiesmooth.nyx.feature.theme.basic.resources.theme_mode_light
import com.slothiesmooth.nyx.feature.theme.basic.resources.theme_palette
import com.slothiesmooth.nyx.feature.theme.basic.resources.theme_title
import org.jetbrains.compose.resources.stringResource

/** The palette + mode picker. Selection state and every choice callback come from [ThemeViewModel]. */
@Composable
fun ChangeThemeScreen(viewModel: ThemeViewModel) {
    ChangeThemeScreenStateless(
        state = viewModel.state,
        onMode = viewModel::onModeSelected,
        onPalette = viewModel::onPaletteSelected,
    )
}

/**
 * Stateless body shared by [ChangeThemeScreen] and its previews. Every rendered value comes from
 * [state]; the composable performs no logic beyond binding state to the design-library atoms.
 */
@Composable
internal fun ChangeThemeScreenStateless(
    state: ChangeThemeState,
    onMode: (ThemeMode) -> Unit = {},
    onPalette: (NxPalette) -> Unit = {},
) {
    val dimensions = MaterialTheme.nxDimensions
    val subtleColor = MaterialTheme.nxColors.fgSubtle
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(dimensions.keyline4),
        verticalArrangement = Arrangement.spacedBy(dimensions.keyline4),
    ) {
        NxText(text = stringResource(Res.string.theme_title), style = NxTextStyle.Title)

        NxText(text = stringResource(Res.string.theme_mode), style = NxTextStyle.Kicker, color = subtleColor)
        Row(horizontalArrangement = Arrangement.spacedBy(dimensions.keyline2)) {
            ThemeMode.entries.forEach { mode ->
                NxChip(
                    text = mode.name,
                    selected = state.mode == mode,
                    onClick = { onMode(mode) },
                )
            }
        }

        NxText(text = stringResource(Res.string.theme_palette), style = NxTextStyle.Kicker, color = subtleColor)
        state.palettes.forEach { palette ->
            val selected = palette == state.darkPalette || palette == state.lightPalette
            NxCard(
                modifier = Modifier.fillMaxWidth(),
                variant = if (selected) NxCardVariant.Elevated else NxCardVariant.Flat,
                onClick = { onPalette(palette) },
            ) {
                NxText(text = palette.displayName, style = NxTextStyle.BodyStrong)
                NxText(
                    text = if (palette.dark) {
                        stringResource(Res.string.theme_mode_dark)
                    } else {
                        stringResource(Res.string.theme_mode_light)
                    },
                    style = NxTextStyle.Caption,
                    color = subtleColor,
                )
            }
        }
    }
}
