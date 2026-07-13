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
        assertEquals(Color(0xFF14121F), NxColorsUmbra.bg)
        assertEquals(Color(0xFF000000), NxColorsEclipse.bg)
        assertEquals(Color(0xFF1D1430), NxColorsDusk.bg)
        assertEquals(Color(0xFFF4F4F8), NxColorsMoonlight.bg)
        assertEquals(Color(0xFFFAF5EC), NxColorsDawn.bg)
    }

    @Test
    fun `palette fg slots match locked hex`() {
        assertEquals(Color(0xFFECE9F6), NxColorsUmbra.fg)
        assertEquals(Color(0xFFF2F2F7), NxColorsEclipse.fg)
        assertEquals(Color(0xFFF0EAF9), NxColorsDusk.fg)
        assertEquals(Color(0xFF1B1926), NxColorsMoonlight.fg)
        assertEquals(Color(0xFF26201A), NxColorsDawn.fg)
    }

    @Test
    fun `palette brand slots match locked hex`() {
        assertEquals(Color(0xFFA78BFA), NxColorsUmbra.brand)
        assertEquals(Color(0xFFB8A6FF), NxColorsEclipse.brand)
        assertEquals(Color(0xFFC084FC), NxColorsDusk.brand)
        assertEquals(Color(0xFF6D28D9), NxColorsMoonlight.brand)
        assertEquals(Color(0xFF7E22CE), NxColorsDawn.brand)
    }

    @Test
    fun `palette enum wiring and defaults`() {
        assertEquals(NxPalette.Umbra, NxPalette.DefaultDark)
        assertEquals(NxPalette.Moonlight, NxPalette.DefaultLight)
        assertTrue(NxPalette.Umbra.dark)
        assertTrue(NxPalette.Eclipse.dark)
        assertTrue(NxPalette.Dusk.dark)
        assertFalse(NxPalette.Moonlight.dark)
        assertFalse(NxPalette.Dawn.dark)
        assertEquals(NxColorsUmbra, NxPalette.Umbra.colors)
        assertEquals(NxColorsEclipse, NxPalette.Eclipse.colors)
        assertEquals(NxColorsDusk, NxPalette.Dusk.colors)
        assertEquals(NxColorsMoonlight, NxPalette.Moonlight.colors)
        assertEquals(NxColorsDawn, NxPalette.Dawn.colors)
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
