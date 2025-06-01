package com.rafambn.kmap.components

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultBlendMode
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import com.rafambn.kmap.core.DrawPosition
import com.rafambn.kmap.tiles.TileRenderResult
import com.rafambn.kmap.utils.Degrees
import com.rafambn.kmap.utils.Coordinates

sealed interface Parameters

open class MarkerParameters(
    val coordinates: Coordinates,
    val alpha: Float = 1F,
    val drawPosition: DrawPosition = DrawPosition.TOP_LEFT,
    val zIndex: Float = 2F,
    val zoomVisibilityRange: ClosedFloatingPointRange<Float> = 0F..Float.MAX_VALUE,
    val zoomToFix: Float? = null,
    val rotateWithMap: Boolean = false,
    val rotation: Degrees = 0.0,
    val clusterId: Int? = null
) : Parameters

open class ClusterParameters(
    val id: Int,
    val alpha: Float = 1F,
    val zIndex: Float = 2F,
    val rotateWithMap: Boolean = false,
    val rotation: Degrees = 0.0
) : Parameters

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
    val zoomToFix: Float? = null
) : Parameters

open class CanvasParameters(
    val id: Int,
    val alpha: Float = 1F,
    val zIndex: Float = 0F,
    val maxCacheTiles: Int = 20,
    val getTile: suspend (zoom: Int, row: Int, column: Int) -> TileRenderResult,
) : Parameters
