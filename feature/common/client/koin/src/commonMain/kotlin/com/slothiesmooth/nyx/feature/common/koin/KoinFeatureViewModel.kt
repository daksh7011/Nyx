package com.slothiesmooth.nyx.feature.common.koin

import androidx.compose.runtime.Composable
import com.slothiesmooth.nyx.shared.presentation.viewmodel.BaseViewModel
import org.koin.compose.viewmodel.koinViewModel

/** Resolves a feature ViewModel from the isolated graph and wires its lifecycle via [BaseViewModel.bind]. */
@Composable
inline fun <reified T : BaseViewModel> koinFeatureViewModel(key: String? = null): T {
    val viewModel = koinViewModel<T>(key = key)
    viewModel.bind()
    return viewModel
}
