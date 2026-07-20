package com.slothiesmooth.nyx.designlibrary.tokens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The 4.dp keyline grid: `keylineN = (N*4).dp`, half-step `keylineNH = (N*4+2).dp`. */
@Immutable
data class Dimensions(
    val zero: Dp = 0.dp,
    val one: Dp = 1.dp,
    val keyline0: Dp = 2.dp,
    val keyline1: Dp = 4.dp,
    val keyline1H: Dp = 6.dp,
    val keyline2: Dp = 8.dp,
    val keyline2H: Dp = 10.dp,
    val keyline3: Dp = 12.dp,
    val keyline3H: Dp = 14.dp,
    val keyline4: Dp = 16.dp,
    val keyline5: Dp = 20.dp,
    val keyline6: Dp = 24.dp,
    val keyline7: Dp = 28.dp,
    val keyline8: Dp = 32.dp,
    val keyline10: Dp = 40.dp,
    val keyline12: Dp = 48.dp,
    val keyline16: Dp = 64.dp,
    val keyline24: Dp = 96.dp,
    val keyline32: Dp = 128.dp,
)

val LocalDimensions: ProvidableCompositionLocal<Dimensions> = staticCompositionLocalOf { Dimensions() }

val MaterialTheme.nxDimensions: Dimensions
    @Composable
    @ReadOnlyComposable
    get() = LocalDimensions.current
