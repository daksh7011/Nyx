package com.slothiesmooth.nyx.designlibrary.tokens

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

private const val FIXED_DIM_ALPHA = 0.6f
private const val ERROR_CONTAINER_ALPHA = 0.18f

/**
 * Maps Nx tokens onto Material3 slots so stock M3 widgets (AlertDialog, Snackbar, ...)
 * visually match Nx components without consuming Nx tokens directly.
 */
fun NxColors.toMaterial3ColorScheme(dark: Boolean): ColorScheme {
    return if (dark) {
        darkMaterialScheme()
    } else {
        lightMaterialScheme()
    }
}

private fun NxColors.darkMaterialScheme(): ColorScheme = darkColorScheme(
    primary = brand,
    onPrimary = brandFg,
    primaryContainer = brandSoft,
    onPrimaryContainer = fgOnBrand,
    primaryFixed = brand,
    primaryFixedDim = brand.copy(alpha = FIXED_DIM_ALPHA),
    onPrimaryFixed = brandFg,
    onPrimaryFixedVariant = fgOnBrand,
    inversePrimary = brand,
    secondary = accentCyan,
    onSecondary = fg,
    secondaryContainer = bgElev2,
    onSecondaryContainer = fg,
    secondaryFixed = accentCyan,
    secondaryFixedDim = accentCyan.copy(alpha = FIXED_DIM_ALPHA),
    onSecondaryFixed = fg,
    onSecondaryFixedVariant = fgMuted,
    tertiary = accentViolet,
    onTertiary = fg,
    tertiaryContainer = bgElev2,
    onTertiaryContainer = fg,
    tertiaryFixed = accentViolet,
    tertiaryFixedDim = accentViolet.copy(alpha = FIXED_DIM_ALPHA),
    onTertiaryFixed = fg,
    onTertiaryFixedVariant = fgMuted,
    background = bg,
    onBackground = fg,
    surface = bgElev1,
    onSurface = fg,
    surfaceVariant = bgElev2,
    onSurfaceVariant = fgMuted,
    surfaceTint = brand,
    inverseSurface = bgInverse,
    inverseOnSurface = fgInverse,
    error = danger,
    onError = bg,
    errorContainer = danger.copy(alpha = ERROR_CONTAINER_ALPHA),
    onErrorContainer = danger,
    outline = border,
    outlineVariant = divider,
    scrim = bg,
    surfaceBright = bgElev2,
    surfaceDim = bgSunken,
    surfaceContainer = bgElev1,
    surfaceContainerHigh = bgElev2,
    surfaceContainerHighest = bgElev2,
    surfaceContainerLow = bg,
    surfaceContainerLowest = bgSunken,
)

private fun NxColors.lightMaterialScheme(): ColorScheme = lightColorScheme(
    primary = brand,
    onPrimary = brandFg,
    primaryContainer = brandSoft,
    onPrimaryContainer = fgOnBrand,
    primaryFixed = brand,
    primaryFixedDim = brand.copy(alpha = FIXED_DIM_ALPHA),
    onPrimaryFixed = brandFg,
    onPrimaryFixedVariant = fgOnBrand,
    inversePrimary = brand,
    secondary = accentCyan,
    onSecondary = fg,
    secondaryContainer = bgElev2,
    onSecondaryContainer = fg,
    secondaryFixed = accentCyan,
    secondaryFixedDim = accentCyan.copy(alpha = FIXED_DIM_ALPHA),
    onSecondaryFixed = fg,
    onSecondaryFixedVariant = fgMuted,
    tertiary = accentViolet,
    onTertiary = fg,
    tertiaryContainer = bgElev2,
    onTertiaryContainer = fg,
    tertiaryFixed = accentViolet,
    tertiaryFixedDim = accentViolet.copy(alpha = FIXED_DIM_ALPHA),
    onTertiaryFixed = fg,
    onTertiaryFixedVariant = fgMuted,
    background = bg,
    onBackground = fg,
    surface = bgElev1,
    onSurface = fg,
    surfaceVariant = bgElev2,
    onSurfaceVariant = fgMuted,
    surfaceTint = brand,
    inverseSurface = bgInverse,
    inverseOnSurface = fgInverse,
    error = danger,
    onError = bgElev2,
    errorContainer = danger.copy(alpha = ERROR_CONTAINER_ALPHA),
    onErrorContainer = danger,
    outline = border,
    outlineVariant = divider,
    scrim = bg,
    surfaceBright = bgElev2,
    surfaceDim = bgSunken,
    surfaceContainer = bgElev1,
    surfaceContainerHigh = bgElev2,
    surfaceContainerHighest = bgElev2,
    surfaceContainerLow = bg,
    surfaceContainerLowest = bgSunken,
)
