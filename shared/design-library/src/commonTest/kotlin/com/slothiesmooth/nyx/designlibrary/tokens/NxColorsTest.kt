package com.slothiesmooth.nyx.designlibrary.tokens

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NxColorsTest {

    @Test
    fun `palette bg slots match locked hex`() {
        assertEquals(Color(0xFF14121F), NxColorsMidnight.bg)
        assertEquals(Color(0xFF000000), NxColorsEspresso.bg)
        assertEquals(Color(0xFF1D1430), NxColorsNardo.bg)
        assertEquals(Color(0xFFF4F4F8), NxColorsCreame.bg)
        assertEquals(Color(0xFFF1E8D7), NxColorsMist.bg)
    }

    @Test
    fun `palette fg slots match locked hex`() {
        assertEquals(Color(0xFFECE9F6), NxColorsMidnight.fg)
        assertEquals(Color(0xFFF2F2F7), NxColorsEspresso.fg)
        assertEquals(Color(0xFFF0EAF9), NxColorsNardo.fg)
        assertEquals(Color(0xFF1B1926), NxColorsCreame.fg)
        assertEquals(Color(0xFF26201A), NxColorsMist.fg)
    }

    @Test
    fun `palette brand slots match locked hex`() {
        assertEquals(Color(0xFFA78BFA), NxColorsMidnight.brand)
        assertEquals(Color(0xFFB8A6FF), NxColorsEspresso.brand)
        assertEquals(Color(0xFFC084FC), NxColorsNardo.brand)
        assertEquals(Color(0xFF6D28D9), NxColorsCreame.brand)
        assertEquals(Color(0xFF7E22CE), NxColorsMist.brand)
    }

    @Test
    fun `palette enum wiring and defaults`() {
        assertEquals(NxPalette.Midnight, NxPalette.DefaultDark)
        assertEquals(NxPalette.Creame, NxPalette.DefaultLight)
        assertTrue(NxPalette.Midnight.dark)
        assertTrue(NxPalette.Espresso.dark)
        assertTrue(NxPalette.Nardo.dark)
        assertFalse(NxPalette.Creame.dark)
        assertFalse(NxPalette.Mist.dark)
        assertEquals(NxColorsMidnight, NxPalette.Midnight.colors)
        assertEquals(NxColorsEspresso, NxPalette.Espresso.colors)
        assertEquals(NxColorsNardo, NxPalette.Nardo.colors)
        assertEquals(NxColorsCreame, NxPalette.Creame.colors)
        assertEquals(NxColorsMist, NxPalette.Mist.colors)
    }

    @Test
    fun `every palette meets WCAG AA on core foreground-background pairs`() {
        NxPalette.entries.forEach { palette ->
            val c = palette.colors
            val pairs = listOf(
                Triple("fg on bg", c.fg, c.bg),
                Triple("fg on bgElev1", c.fg, c.bgElev1),
                Triple("fg on bgElev2", c.fg, c.bgElev2),
                Triple("fgMuted on bg", c.fgMuted, c.bg),
                Triple("brandFg on brand", c.brandFg, c.brand),
                Triple("fgOnBrand on brand", c.fgOnBrand, c.brand),
                Triple("fgOnBrand on danger", c.fgOnBrand, c.danger),
                Triple("danger on bg", c.danger, c.bg),
                Triple("success on bg", c.success, c.bg),
                Triple("warning on bg", c.warning, c.bg),
                Triple("info on bg", c.info, c.bg),
                Triple("codeFg on codeBg", c.codeFg, c.codeBg),
            )
            pairs.forEach { (label, front, back) ->
                val ratio = contrastRatio(front, back)
                assertTrue(
                    ratio >= WCAG_AA_MIN_RATIO,
                    "${palette.name}: $label is $ratio, expected >= $WCAG_AA_MIN_RATIO",
                )
            }
        }
    }
}

private const val WCAG_AA_MIN_RATIO = 4.5

private fun linearize(channel: Float): Double {
    val value = channel.toDouble()
    return if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
}

private fun relativeLuminance(color: Color): Double =
    0.2126 * linearize(color.red) + 0.7152 * linearize(color.green) + 0.0722 * linearize(color.blue)

private fun contrastRatio(a: Color, b: Color): Double {
    val luminanceA = relativeLuminance(a)
    val luminanceB = relativeLuminance(b)
    val lighter = maxOf(luminanceA, luminanceB)
    val darker = minOf(luminanceA, luminanceB)
    return (lighter + 0.05) / (darker + 0.05)
}
