package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxType

@Composable
fun NxText(
    text: String,
    modifier: Modifier = Modifier,
    style: NxTextStyle = NxTextStyle.Body,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null,
) {
    val resolvedColor = if (color == Color.Unspecified) MaterialTheme.nxColors.fg else color
    val textStyle = when (style) {
        NxTextStyle.Display -> MaterialTheme.nxType.display
        NxTextStyle.Title -> MaterialTheme.nxType.title
        NxTextStyle.Heading -> MaterialTheme.nxType.heading
        NxTextStyle.Subhead -> MaterialTheme.nxType.subhead
        NxTextStyle.Body -> MaterialTheme.nxType.body
        NxTextStyle.BodyStrong -> MaterialTheme.nxType.bodyStrong
        NxTextStyle.Caption -> MaterialTheme.nxType.caption
        NxTextStyle.Kicker -> MaterialTheme.nxType.kicker
        NxTextStyle.Mono -> MaterialTheme.nxType.mono
    }
    Text(
        text = if (style == NxTextStyle.Kicker) text.uppercase() else text,
        modifier = modifier,
        color = resolvedColor,
        style = textStyle,
        maxLines = maxLines,
        textAlign = textAlign,
    )
}
