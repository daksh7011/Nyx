package com.slothiesmooth.nyx.designlibrary.tokens

import androidx.compose.ui.tooling.preview.Preview

// Documented exception (spec section 8): @Preview backgroundColor requires compile-time Long
// constants; these mirror each palette's NxColors.bg and are locked by NxColorsTest.
private const val BG_MIDNIGHT = 0xFF14121F
private const val BG_ESPRESSO = 0xFF000000
private const val BG_NARDO = 0xFF1D1430
private const val BG_CREAME = 0xFFF4F4F8
private const val BG_MIST = 0xFFFAF5EC

@Preview(name = "midnight", group = "dark", showBackground = true, backgroundColor = BG_MIDNIGHT)
@Preview(name = "espresso", group = "dark", showBackground = true, backgroundColor = BG_ESPRESSO)
@Preview(name = "nardo", group = "dark", showBackground = true, backgroundColor = BG_NARDO)
@Preview(name = "creame", group = "light", showBackground = true, backgroundColor = BG_CREAME)
@Preview(name = "mist", group = "light", showBackground = true, backgroundColor = BG_MIST)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
annotation class AllThemePreview
