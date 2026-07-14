package com.slothiesmooth.nyx.designlibrary.tokens

import androidx.compose.ui.tooling.preview.Preview

// Documented exception (spec section 8): @Preview backgroundColor requires compile-time Long
// constants; these mirror each palette's NxColors.bg and are locked by NxColorsTest.
private const val BG_MIDNIGHT = 0xFF0F1318
private const val BG_ESPRESSO = 0xFF1D1814
private const val BG_NARDO = 0xFF1E2020
private const val BG_CREAM = 0xFFF3EFE4
private const val BG_MIST = 0xFFF1F4F0

@Preview(name = "midnight", group = "dark", showBackground = true, backgroundColor = BG_MIDNIGHT)
@Preview(name = "espresso", group = "dark", showBackground = true, backgroundColor = BG_ESPRESSO)
@Preview(name = "nardo", group = "dark", showBackground = true, backgroundColor = BG_NARDO)
@Preview(name = "cream", group = "light", showBackground = true, backgroundColor = BG_CREAM)
@Preview(name = "mist", group = "light", showBackground = true, backgroundColor = BG_MIST)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
annotation class AllThemePreview
