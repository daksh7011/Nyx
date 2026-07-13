package com.slothiesmooth.nyx.designlibrary.tokens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Corner radii, keyline-aligned. */
@Immutable
data class Corners(
    val xs: Dp = 6.dp,
    val sm: Dp = 10.dp,
    val md: Dp = 14.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 28.dp,
    val xxl: Dp = 40.dp,
    val pill: Dp = 999.dp,
)

val LocalCorners: ProvidableCompositionLocal<Corners> = staticCompositionLocalOf { Corners() }

val MaterialTheme.nxCorners: Corners
    @Composable
    @ReadOnlyComposable
    get() = LocalCorners.current
