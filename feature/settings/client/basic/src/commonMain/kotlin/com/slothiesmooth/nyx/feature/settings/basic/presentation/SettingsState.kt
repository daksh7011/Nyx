package com.slothiesmooth.nyx.feature.settings.basic.presentation

import androidx.compose.runtime.Stable
import com.slothiesmooth.nyx.shared.presentation.state.ViewState

/** Read-only settings-screen state the screen observes: about label plus the wipe-confirm flow. */
@Stable
interface SettingsState : ViewState {
    val versionLabel: String
    val showWipeConfirm: Boolean
    val isWiping: Boolean
}
