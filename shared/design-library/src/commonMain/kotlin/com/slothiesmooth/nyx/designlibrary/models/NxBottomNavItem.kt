package com.slothiesmooth.nyx.designlibrary.models

import androidx.compose.runtime.Immutable
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind

/** One bottom-navigation destination: an [icon] over its [label]. Selection lives in the caller. */
@Immutable
data class NxBottomNavItem(
    val icon: NxIconKind,
    val label: String,
)
