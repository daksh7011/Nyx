package com.slothiesmooth.nyx.designlibrary.tokens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/** Shared opacity levels for disabled, medium-emphasis, and divider surfaces. */
@Immutable
data class Alpha(
    val disabled: Float = 0.38f,
    val medium: Float = 0.60f,
    val divider: Float = 0.12f,
)

val LocalAlpha: ProvidableCompositionLocal<Alpha> = staticCompositionLocalOf { Alpha() }

val MaterialTheme.nxAlpha: Alpha
    @Composable
    @ReadOnlyComposable
    get() = LocalAlpha.current
