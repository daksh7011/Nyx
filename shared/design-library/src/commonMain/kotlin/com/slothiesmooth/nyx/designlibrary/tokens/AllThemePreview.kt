package com.slothiesmooth.nyx.designlibrary.tokens

import org.jetbrains.compose.ui.tooling.preview.Preview

// Documented exception (spec section 8): @Preview backgroundColor requires compile-time Long
// constants; these mirror each palette's NxColors.bg and are locked by NxColorsTest.
private const val BG_UMBRA = 0xFF14121F
private const val BG_ECLIPSE = 0xFF000000
private const val BG_DUSK = 0xFF1D1430
private const val BG_MOONLIGHT = 0xFFF4F4F8
private const val BG_DAWN = 0xFFFAF5EC

@Preview(name = "umbra", group = "dark", showBackground = true, backgroundColor = BG_UMBRA)
@Preview(name = "eclipse", group = "dark", showBackground = true, backgroundColor = BG_ECLIPSE)
@Preview(name = "dusk", group = "dark", showBackground = true, backgroundColor = BG_DUSK)
@Preview(name = "moonlight", group = "light", showBackground = true, backgroundColor = BG_MOONLIGHT)
@Preview(name = "dawn", group = "light", showBackground = true, backgroundColor = BG_DAWN)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
annotation class AllThemePreview
