package com.slothiesmooth.nyx.feature.splash.basic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.atoms.NxIcon
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions

private val BrandMarkSize: Dp = 72.dp

/** The brand splash. Calls [onReady] exactly once, when the ViewModel signals the dwell is done. */
@Composable
fun SplashScreen(viewModel: SplashViewModel, onReady: () -> Unit) {
    val ready = viewModel.state.ready
    LaunchedEffect(ready) { if (ready) onReady() }
    SplashScreenStateless()
}

@Composable
internal fun SplashScreenStateless() {
    val colors = MaterialTheme.nxColors
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline4, Alignment.CenterVertically),
    ) {
        NxIcon(kind = NxIconKind.Vault, tint = colors.brand, size = BrandMarkSize)
        NxText(text = "Nyx", style = NxTextStyle.Display, color = colors.fg)
    }
}
