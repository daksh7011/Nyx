package com.slothiesmooth.nyx.designlibrary.tokens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalNxColors: ProvidableCompositionLocal<NxColors> = staticCompositionLocalOf {
    error("NxColors not provided. Wrap content in NxTheme.")
}

val MaterialTheme.nxColors: NxColors
    @Composable
    @ReadOnlyComposable
    get() = LocalNxColors.current

@Composable
fun NxTheme(
    palette: NxPalette = NxPalette.DefaultDark,
    content: @Composable () -> Unit,
) {
    val colors = palette.colors
    CompositionLocalProvider(
        LocalNxColors provides colors,
        LocalNxType provides nxTypeFor(rememberNxJetBrainsMono()),
        LocalDimensions provides Dimensions(),
        LocalCorners provides Corners(),
        LocalAlpha provides Alpha(),
        LocalElevations provides Elevations(),
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterial3ColorScheme(dark = palette.dark),
            content = content,
        )
    }
}
