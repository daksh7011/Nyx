package com.slothiesmooth.nyx.designlibrary.organisms

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.slothiesmooth.nyx.designlibrary.models.NxBottomNavItem
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import kotlinx.collections.immutable.persistentListOf

@Composable
fun NxBottomNavSample() {
    NxBottomNav(
        items = persistentListOf(
            NxBottomNavItem(NxIconKind.Vault, "Vault"),
            NxBottomNavItem(NxIconKind.Lock, "Encrypt"),
            NxBottomNavItem(NxIconKind.Unlock, "Decrypt"),
            NxBottomNavItem(NxIconKind.Settings, "Settings"),
        ),
        selectedIndex = 0,
        onSelect = {},
    )
}

@AllThemePreview
@Composable
private fun NxBottomNavPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxBottomNavSample() }
}
