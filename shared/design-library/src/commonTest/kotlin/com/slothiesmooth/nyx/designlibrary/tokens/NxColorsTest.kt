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
        assertEquals(Color(0xFF0F1318), NxColorsMidnight.bg)
        assertEquals(Color(0xFF1D1814), NxColorsEspresso.bg)
        assertEquals(Color(0xFF1E2020), NxColorsNardo.bg)
        assertEquals(Color(0xFFF3EFE4), NxColorsCream.bg)
        assertEquals(Color(0xFFF1F4F0), NxColorsMist.bg)
    }

    @Test
    fun `palette fg slots match locked hex`() {
        assertEquals(Color(0xFFE3E8EC), NxColorsMidnight.fg)
        assertEquals(Color(0xFFEFEADF), NxColorsEspresso.fg)
        assertEquals(Color(0xFFEDEFE9), NxColorsNardo.fg)
        assertEquals(Color(0xFF1C1F1F), NxColorsCream.fg)
        assertEquals(Color(0xFF1A1F1C), NxColorsMist.fg)
    }

    @Test
    fun `palette brand slots match locked hex`() {
        assertEquals(Color(0xFFA7D2B6), NxColorsMidnight.brand)
        assertEquals(Color(0xFFC8D9AA), NxColorsEspresso.brand)
        assertEquals(Color(0xFFBFDCC9), NxColorsNardo.brand)
        assertEquals(Color(0xFF4E7761), NxColorsCream.brand)
        assertEquals(Color(0xFF4E7761), NxColorsMist.brand)
    }

    @Test
    fun `palette enum wiring and defaults`() {
        assertEquals(NxPalette.Nardo, NxPalette.DefaultDark)
        assertEquals(NxPalette.Cream, NxPalette.DefaultLight)
        assertTrue(NxPalette.Midnight.dark)
        assertTrue(NxPalette.Espresso.dark)
        assertTrue(NxPalette.Nardo.dark)
        assertFalse(NxPalette.Cream.dark)
        assertFalse(NxPalette.Mist.dark)
        assertEquals(NxColorsMidnight, NxPalette.Midnight.colors)
        assertEquals(NxColorsEspresso, NxPalette.Espresso.colors)
        assertEquals(NxColorsNardo, NxPalette.Nardo.colors)
        assertEquals(NxColorsCream, NxPalette.Cream.colors)
        assertEquals(NxColorsMist, NxPalette.Mist.colors)
    }

    @Test
    fun `body and code text meet WCAG AA on their surfaces`() {
        NxPalette.entries.forEach { palette ->
            val c = palette.colors
            val pairs = listOf(
                Triple("fg on bg", c.fg, c.bg),
                Triple("fg on bgElev1", c.fg, c.bgElev1),
                Triple("fg on bgElev2", c.fg, c.bgElev2),
                Triple("fgMuted on bg", c.fgMuted, c.bg),
                Triple("codeFg on codeBg", c.codeFg, c.codeBg),
            )
            pairs.forEach { (label, front, back) ->
                val ratio = contrastRatio(front, back)
                assertTrue(
                    ratio >= WCAG_AA_TEXT,
                    "${palette.name}: $label is $ratio, expected >= $WCAG_AA_TEXT",
                )
            }
        }
    }

    @Test
    fun `brand labels meet WCAG AA large-text contrast`() {
        // Button labels (brandFg on brand) sit at the design system's 3:1 large-text/UI floor, not
        // the 4.5 body-text minimum. Signal colors (warning/success/info/danger) are fills and
        // icons in this system, never asserted as body text on bg.
        NxPalette.entries.forEach { palette ->
            val c = palette.colors
            val ratio = contrastRatio(c.brandFg, c.brand)
            assertTrue(
                ratio >= WCAG_AA_LARGE,
                "${palette.name}: brandFg on brand is $ratio, expected >= $WCAG_AA_LARGE",
            )
        }
    }
}

private const val WCAG_AA_TEXT = 4.5
private const val WCAG_AA_LARGE = 3.0

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
