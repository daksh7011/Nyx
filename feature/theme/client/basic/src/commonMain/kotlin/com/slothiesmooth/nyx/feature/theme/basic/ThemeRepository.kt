package com.slothiesmooth.nyx.feature.theme.basic

import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.feature.theme.api.ThemeConfig
import com.slothiesmooth.nyx.feature.theme.api.ThemeMode
import com.slothiesmooth.nyx.shared.data.source.SettingsSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

private const val KEY_MODE = "theme.mode"
private const val KEY_DARK = "theme.dark"
private const val KEY_LIGHT = "theme.light"

/** Persists and observes the [ThemeConfig] behind a [SettingsSource]. */
class ThemeRepository(private val settings: SettingsSource) {

    fun observeConfig(): Flow<ThemeConfig> = combine(
        settings.observeString(KEY_MODE),
        settings.observeString(KEY_DARK),
        settings.observeString(KEY_LIGHT),
    ) { mode, dark, light ->
        ThemeConfig(
            mode = parseMode(mode),
            darkPalette = parsePalette(dark, dark = true),
            lightPalette = parsePalette(light, dark = false),
        )
    }

    suspend fun setMode(mode: ThemeMode) = settings.putString(KEY_MODE, mode.name)

    suspend fun setPalette(palette: NxPalette) =
        settings.putString(if (palette.dark) KEY_DARK else KEY_LIGHT, palette.name)

    private fun parseMode(raw: String?): ThemeMode =
        ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.System

    private fun parsePalette(raw: String?, dark: Boolean): NxPalette {
        val fallback = if (dark) NxPalette.DefaultDark else NxPalette.DefaultLight
        val parsed = NxPalette.entries.firstOrNull { it.name == raw } ?: return fallback
        return if (parsed.dark == dark) parsed else fallback
    }
}
