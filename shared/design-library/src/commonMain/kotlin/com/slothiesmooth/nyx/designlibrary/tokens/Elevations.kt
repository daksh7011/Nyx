package com.slothiesmooth.nyx.designlibrary.tokens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Shadow elevation levels, applied via `Modifier.shadow(elevation, shape)`. */
@Immutable
data class Elevations(
    val level0: Dp = 0.dp,
    val level1: Dp = 1.dp,
    val level2: Dp = 3.dp,
    val level3: Dp = 6.dp,
    val level4: Dp = 8.dp,
    val level5: Dp = 12.dp,
)

val LocalElevations: ProvidableCompositionLocal<Elevations> = staticCompositionLocalOf { Elevations() }

val MaterialTheme.nxElevations: Elevations
    @Composable
    @ReadOnlyComposable
    get() = LocalElevations.current
