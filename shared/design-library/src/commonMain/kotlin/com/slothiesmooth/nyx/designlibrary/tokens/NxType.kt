package com.slothiesmooth.nyx.designlibrary.tokens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.slothiesmooth.nyx.designlibrary.resources.Res
import com.slothiesmooth.nyx.designlibrary.resources.jetbrainsmono_bold
import com.slothiesmooth.nyx.designlibrary.resources.jetbrainsmono_italic
import com.slothiesmooth.nyx.designlibrary.resources.jetbrainsmono_medium
import com.slothiesmooth.nyx.designlibrary.resources.jetbrainsmono_regular
import com.slothiesmooth.nyx.designlibrary.resources.jetbrainsmono_semibold
import org.jetbrains.compose.resources.Font

@Immutable
data class NxType(
    val display: TextStyle,
    val title: TextStyle,
    val heading: TextStyle,
    val subhead: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val caption: TextStyle,
    val kicker: TextStyle,
    val mono: TextStyle,
)

@Composable
fun rememberNxJetBrainsMono(): FontFamily {
    val regular = Font(Res.font.jetbrainsmono_regular, weight = FontWeight.Normal)
    val medium = Font(Res.font.jetbrainsmono_medium, weight = FontWeight.Medium)
    val semibold = Font(Res.font.jetbrainsmono_semibold, weight = FontWeight.SemiBold)
    val bold = Font(Res.font.jetbrainsmono_bold, weight = FontWeight.Bold)
    val italic = Font(Res.font.jetbrainsmono_italic, weight = FontWeight.Normal, style = FontStyle.Italic)
    return remember(regular, medium, semibold, bold, italic) {
        FontFamily(regular, medium, semibold, bold, italic)
    }
}

private val displaySize = 36.sp
private val displayLine = 38.sp
private val titleSize = 28.sp
private val titleLine = 32.sp
private val headingSize = 22.sp
private val headingLine = 26.sp
private val subheadSize = 18.sp
private val subheadLine = 22.sp
private val bodySize = 14.sp
private val bodyLine = 20.sp
private val captionSize = 12.sp
private val captionLine = 16.sp
private val kickerSize = 11.sp
private val kickerLine = 14.sp
private val monoSize = 28.sp
private val monoLine = 30.sp
private val tightTracking = (-0.04).em
private val titleTracking = (-0.025).em
private val headingTracking = (-0.02).em
private val kickerTracking = 0.14.em

fun nxTypeFor(family: FontFamily): NxType = NxType(
    display = TextStyle(
        fontFamily = family,
        fontSize = displaySize,
        fontWeight = FontWeight.Bold,
        letterSpacing = tightTracking,
        lineHeight = displayLine,
    ),
    title = TextStyle(
        fontFamily = family,
        fontSize = titleSize,
        fontWeight = FontWeight.Bold,
        letterSpacing = titleTracking,
        lineHeight = titleLine,
    ),
    heading = TextStyle(
        fontFamily = family,
        fontSize = headingSize,
        fontWeight = FontWeight.Bold,
        letterSpacing = headingTracking,
        lineHeight = headingLine,
    ),
    subhead = TextStyle(
        fontFamily = family,
        fontSize = subheadSize,
        fontWeight = FontWeight.Bold,
        letterSpacing = headingTracking,
        lineHeight = subheadLine,
    ),
    body = TextStyle(fontFamily = family, fontSize = bodySize, fontWeight = FontWeight.Normal, lineHeight = bodyLine),
    bodyStrong = TextStyle(
        fontFamily = family,
        fontSize = bodySize,
        fontWeight = FontWeight.SemiBold,
        lineHeight = bodyLine,
    ),
    caption = TextStyle(
        fontFamily = family,
        fontSize = captionSize,
        fontWeight = FontWeight.Normal,
        lineHeight = captionLine,
    ),
    kicker = TextStyle(
        fontFamily = family,
        fontSize = kickerSize,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = kickerTracking,
        lineHeight = kickerLine,
    ),
    mono = TextStyle(
        fontFamily = family,
        fontSize = monoSize,
        fontWeight = FontWeight.Bold,
        fontFeatureSettings = "tnum",
        letterSpacing = tightTracking,
        lineHeight = monoLine,
    ),
)

val LocalNxType: ProvidableCompositionLocal<NxType> = staticCompositionLocalOf { nxTypeFor(FontFamily.Monospace) }

val MaterialTheme.nxType: NxType
    @Composable
    @ReadOnlyComposable
    get() = LocalNxType.current
