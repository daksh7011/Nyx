package com.slothiesmooth.nyx.feature.theme.api

import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.feature.common.api.Feature
import kotlinx.coroutines.flow.StateFlow

/** How the effective palette is chosen: follow the OS, or force one side. */
enum class ThemeMode { System, Light, Dark }

/** The persisted theme selection: the [mode] plus the chosen palette for each side. */
data class ThemeConfig(
    val mode: ThemeMode,
    val darkPalette: NxPalette,
    val lightPalette: NxPalette,
)

/**
 * Cross-feature handle to the app theme. The shell observes [theme] to wrap content in `NxTheme`;
 * the settings/change-theme screen calls [setMode]/[setPalette]. [setPalette] routes the palette to
 * its own side (dark palette -> dark slot, light palette -> light slot).
 */
interface ThemeFeature : Feature {
    val theme: StateFlow<ThemeConfig>
    suspend fun setMode(mode: ThemeMode)
    suspend fun setPalette(palette: NxPalette)
}
