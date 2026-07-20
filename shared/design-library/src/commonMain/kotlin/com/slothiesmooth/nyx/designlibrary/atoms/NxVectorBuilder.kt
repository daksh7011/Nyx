package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private const val VIEWPORT = 24f
private val IconCanvas = 24.dp
private const val STROKE_WIDTH = 1.75f

internal object NxVectorBuilder {

    private fun builder(name: String = "NxIcon") = ImageVector.Builder(
        name = name,
        defaultWidth = IconCanvas,
        defaultHeight = IconCanvas,
        viewportWidth = VIEWPORT,
        viewportHeight = VIEWPORT,
    )

    fun stroke(vararg paths: String): ImageVector {
        val vectorBuilder = builder()
        paths.forEach { pathData ->
            vectorBuilder.addPath(
                pathData = addPathNodes(pathData),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = STROKE_WIDTH,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return vectorBuilder.build()
    }
}
