package com.slothiesmooth.nyx.feature.settings.basic.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slothiesmooth.nyx.feature.settings.basic.domain.usecase.WipeVaultUseCase
import com.slothiesmooth.nyx.shared.data.source.AppInfo
import com.slothiesmooth.nyx.shared.presentation.state.MutableViewState
import com.slothiesmooth.nyx.shared.presentation.viewmodel.BaseViewModel

private class SettingsMutableState(initialVersionLabel: String) : MutableViewState(), SettingsState {
    override var versionLabel: String by mutableStateOf(initialVersionLabel)
    override var showWipeConfirm: Boolean by mutableStateOf(false)
    override var isWiping: Boolean by mutableStateOf(false)
}

/** Drives the settings screen: the About label from [appInfo] plus the wipe-vault confirm/run flow. */
class SettingsViewModel(
    private val wipeVault: WipeVaultUseCase,
    appInfo: AppInfo,
) : BaseViewModel() {

    private val mutableState = SettingsMutableState(versionLabel(appInfo.versionName, appInfo.platformName))
    val state: SettingsState get() = mutableState

    fun requestWipe() = withState { mutableState.showWipeConfirm = true }

    fun cancelWipe() = withState { mutableState.showWipeConfirm = false }

    fun confirmWipe() {
        withState {
            mutableState.showWipeConfirm = false
            mutableState.isWiping = true
        }
        async("wipe") {
            wipeVault()
            withState { mutableState.isWiping = false }
        }
    }
}
