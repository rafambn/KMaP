package com.rafambn.kmap.components.parameters

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultBlendMode
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke

open class PathParameters(
    val path: Path,
    val color: Color,
    val zIndex: Float = 1F,
    val alpha: Float = 1F,
    val style: DrawStyle = Fill,
    val colorFilter: ColorFilter? = null,
    val blendMode: BlendMode = DefaultBlendMode,
    val clickPadding: Float = 10F,
    val zoomVisibilityRange: ClosedFloatingPointRange<Float> = 0F..Float.MAX_VALUE,
    val checkForClickInsidePath: Boolean = false,
) : Parameters {
    internal val totalPadding: Float
        get() = maxOf(if (style is Stroke) style.width / 2F else 0F, clickPadding)
}
